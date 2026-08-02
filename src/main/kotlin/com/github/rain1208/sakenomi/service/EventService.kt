package com.github.rain1208.sakenomi.service

import com.github.rain1208.sakenomi.config.AppConfig
import com.github.rain1208.sakenomi.discord.DiscordGateway
import com.github.rain1208.sakenomi.domain.Event
import com.github.rain1208.sakenomi.domain.EventStatus
import com.github.rain1208.sakenomi.domain.GuildSettings
import com.github.rain1208.sakenomi.domain.ServiceException
import com.github.rain1208.sakenomi.repository.EventRepository
import com.github.rain1208.sakenomi.repository.ParticipantRepository
import com.github.rain1208.sakenomi.util.EventEmbeds
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.interactions.components.buttons.Button
import org.slf4j.LoggerFactory
import java.time.LocalDate

private const val CATEGORY_CHANNEL_LIMIT = 50

class EventService(
    private val config: AppConfig,
    private val eventRepository: EventRepository,
    private val participantRepository: ParticipantRepository,
    private val settingsService: SettingsService,
    private val gateway: DiscordGateway,
    private val auditLogger: AuditLogger,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /** 実行チャンネルからイベントを解決する。イベントチャンネル外ならエラー */
    fun resolveByChannel(guild: Guild, channelId: Long): Event =
        eventRepository.findByChannel(guild.idLong, channelId)
            ?: throw ServiceException("このコマンドはイベントチャンネル内で実行してください")

    fun findById(eventId: Long): Event? = eventRepository.findById(eventId)

    /** Web コンソールのイベント一覧(Web仕様書.md §5.3) */
    fun listByGuild(guildId: Long, status: EventStatus): List<Event> = eventRepository.listByGuild(guildId, status)

    /**
     * イベント作成(§10.2)。
     * Discord リソース生成と DB 登録のいずれかが失敗した場合、生成済みリソースを補償削除する。
     */
    fun create(guild: Guild, organizer: Member, name: String, area: String, eventDate: LocalDate?): Event {
        val settings = settingsService.requireComplete(guild.idLong)
        val category = guild.getCategoryById(settings.eventCategoryId!!)
            ?: throw ServiceException("イベントカテゴリが見つかりません。`/settings set` で再設定してください")
        val recruitChannel = guild.getTextChannelById(settings.recruitChannelId!!)
            ?: throw ServiceException("募集チャンネルが見つかりません。`/settings set` で再設定してください")
        checkCategoryCapacity(guild, category)

        val number = eventRepository.assignNumber(guild.idLong)

        var role: Role? = null
        var channel: TextChannel? = null
        var eventId: Long? = null
        var recruitMessageId: Long? = null
        try {
            role = gateway.createEventRole(guild, number)
            val adminRole = guild.getRoleById(settings.adminRoleId!!)
            channel = gateway.createEventChannel(guild, category, "第${number}回-$name", role, adminRole)

            eventId = eventRepository.insert(
                guildId = guild.idLong,
                number = number,
                name = name,
                area = area,
                eventDate = eventDate,
                channelId = channel.idLong,
                roleId = role.idLong,
                organizerUserId = organizer.idLong,
            )
            val event = eventRepository.findById(eventId)!!

            // 幹事はイベント作成時点で参加者として自動登録する(参加必須)
            participantRepository.add(eventId, organizer.idLong)
            gateway.grantRole(guild, organizer, role.idLong)

            val recruitMessage = postRecruitMessage(recruitChannel, event)
            recruitMessageId = recruitMessage.idLong
            eventRepository.updateRecruitMessageId(eventId, recruitMessage.idLong)

            channel.sendMessageEmbeds(EventEmbeds.welcome(event))
                .setActionRow(Button.danger("event:leave:$eventId", "参加をキャンセル"))
                .queue()

            return event.copy(recruitMessageId = recruitMessage.idLong)
        } catch (e: Exception) {
            logger.error("イベント作成に失敗しました。生成済みリソースを補償削除します (guild={})", guild.idLong, e)
            runCatching { recruitMessageId?.let { gateway.deleteMessage(recruitChannel, it) } }
                .onFailure { logger.error("募集メッセージの補償削除に失敗しました (guild={})", guild.idLong, it) }
            runCatching { channel?.let { gateway.deleteChannel(guild, it.idLong) } }
                .onFailure { logger.error("イベントチャンネルの補償削除に失敗しました (guild={})", guild.idLong, it) }
            runCatching { role?.let { gateway.deleteRole(guild, it.idLong) } }
                .onFailure { logger.error("イベントロールの補償削除に失敗しました (guild={})", guild.idLong, it) }
            runCatching { eventId?.let { eventRepository.delete(it) } }
                .onFailure { logger.error("イベントDBレコードの補償削除に失敗しました (guild={})", guild.idLong, it) }
            if (e is ServiceException) throw e
            throw ServiceException("イベント作成に失敗しました。Bot の権限(ロール管理・チャンネル管理・チャンネルを見る)を確認してください")
        }
    }

    /** イベント編集(§9.1)。チャンネル名は変更せず Embed 表示のみ更新する */
    fun edit(guild: Guild, event: Event, newName: String?, newArea: String?, newDate: LocalDate?): Event {
        val name = newName ?: event.name
        val area = newArea ?: event.area
        val date = newDate ?: event.eventDate
        eventRepository.updateInfo(event.id, name, area, date)
        val updated = event.copy(name = name, area = area, eventDate = date)
        updateRecruitEmbed(guild, updated)
        return updated
    }

    /** 幹事変更(幹事/運営のみ実行可)。新しい幹事が未参加の場合は参加者として自動追加する */
    fun transferOrganizer(guild: Guild, event: Event, executor: Member, newOrganizerUserId: Long): Event {
        if (event.organizerUserId == newOrganizerUserId) {
            throw ServiceException("指定したユーザーは既に幹事です")
        }
        val newOrganizerMember = gateway.findMember(guild.idLong, newOrganizerUserId)
            ?: throw ServiceException("対象ユーザーがサーバーに見つかりません")

        if (!participantRepository.isParticipant(event.id, newOrganizerUserId)) {
            participantRepository.add(event.id, newOrganizerUserId)
            try {
                gateway.grantRole(guild, newOrganizerMember, event.roleId)
            } catch (e: Exception) {
                participantRepository.remove(event.id, newOrganizerUserId)
                throw ServiceException("ロール付与に失敗しました。Bot の権限を確認してください")
            }
        }

        val oldOrganizerUserId = event.organizerUserId
        eventRepository.updateOrganizer(event.id, newOrganizerUserId)
        val updated = event.copy(organizerUserId = newOrganizerUserId)
        updateRecruitEmbed(guild, updated)
        gateway.sendChannelMessage(
            guild,
            event.channelId,
            "[幹事変更] 幹事が ${newOrganizerMember.asMention} に変更されました",
        )
        auditLogger.organizerChanged(guild, updated, oldOrganizerUserId, newOrganizerUserId, executor)
        return updated
    }

    /** 募集メッセージの再送信。埋もれた/削除された募集メッセージを削除し、再度末尾に投稿し直す */
    fun resend(guild: Guild, event: Event): Event {
        if (event.status != EventStatus.RECRUITING) {
            throw ServiceException("募集中のイベントのみ再送信できます")
        }
        val settings = settingsService.requireComplete(guild.idLong)
        val recruitChannel = guild.getTextChannelById(settings.recruitChannelId!!)
            ?: throw ServiceException("募集チャンネルが見つかりません。`/settings set` で再設定してください")

        event.recruitMessageId?.let { gateway.deleteMessage(recruitChannel, it) }

        val recruitMessage = postRecruitMessage(recruitChannel, event)
        eventRepository.updateRecruitMessageId(event.id, recruitMessage.idLong)
        return event.copy(recruitMessageId = recruitMessage.idLong)
    }

    private fun postRecruitMessage(recruitChannel: TextChannel, event: Event) =
        recruitChannel.sendMessageEmbeds(EventEmbeds.recruit(event))
            .setActionRow(
                Button.success("event:join:${event.id}", "参加する"),
                Button.danger("event:leave:${event.id}", "参加をキャンセル"),
            )
            .complete()

    /** アーカイブ化(§10.5) */
    fun archive(guild: Guild, event: Event) {
        if (event.status == EventStatus.ARCHIVED) {
            throw ServiceException("このイベントは既にアーカイブ済みです")
        }
        val settings = settingsService.requireComplete(guild.idLong)
        val archiveCategory = guild.getCategoryById(settings.archiveCategoryId!!)
            ?: throw ServiceException("アーカイブカテゴリが見つかりません。`/settings set` で再設定してください")
        val channel = guild.getTextChannelById(event.channelId)
            ?: throw ServiceException("イベントチャンネルが見つかりません")
        checkCategoryCapacity(guild, archiveCategory)

        gateway.moveToCategory(channel, archiveCategory)

        // 全ロールの送信権限を剥奪(閲覧のみ)。ロール自体は履歴閲覧のため維持する
        val roles = listOfNotNull(
            guild.getRoleById(event.roleId),
            guild.getRoleById(settings.adminRoleId!!),
        )
        gateway.revokeSendPermission(channel, roles)

        disableRecruitButton(guild, settings, event)
        eventRepository.markArchived(event.id)
    }

    /** イベント削除(§10.6)。確認ボタン押下後に呼ばれる */
    fun delete(guild: Guild, event: Event, executor: Member) {
        val settings = settingsService.get(guild.idLong)
        event.recruitMessageId?.let { messageId ->
            settings.recruitChannelId
                ?.let { guild.getTextChannelById(it) }
                ?.let { gateway.deleteMessage(it, messageId) }
        }
        gateway.deleteRole(guild, event.roleId)
        eventRepository.delete(event.id)
        auditLogger.eventDeleted(guild, event, executor)
        // 実行元チャンネルの可能性があるため最後に削除する
        gateway.deleteChannel(guild, event.channelId)
    }

    /** 手動採番修正(§7)。チャンネル名・ロール名・募集メッセージも追随させる */
    fun renumber(guild: Guild, event: Event, newNumber: Int, executor: Member): Event {
        if (event.number == newNumber) {
            throw ServiceException("既に第${newNumber}回です")
        }
        if (eventRepository.numberExists(guild.idLong, newNumber)) {
            throw ServiceException("番号 $newNumber は既に使用されています")
        }
        val oldNumber = event.number
        eventRepository.updateNumber(event.id, newNumber)
        val updated = event.copy(number = newNumber)

        guild.getTextChannelById(event.channelId)?.let {
            gateway.renameChannel(it, "第${newNumber}回-${event.name}")
        }
        guild.getRoleById(event.roleId)?.let {
            gateway.renameRole(it, "第${newNumber}回参加者")
        }
        updateRecruitEmbed(guild, updated)
        auditLogger.renumbered(guild, updated, oldNumber, newNumber, executor)
        return updated
    }

    /** カテゴリ内チャンネル上限対応(§10.8) */
    private fun checkCategoryCapacity(guild: Guild, category: Category) {
        val count = gateway.countChannels(category)
        if (count >= CATEGORY_CHANNEL_LIMIT) {
            throw ServiceException(
                "カテゴリ「${category.name}」がチャンネル上限($CATEGORY_CHANNEL_LIMIT)に達しています。" +
                    "古いイベントを削除するか別カテゴリを設定してください",
            )
        }
        if (count >= config.channelWarnThreshold) {
            auditLogger.capacityWarning(guild, category.name, count)
        }
    }

    private fun updateRecruitEmbed(guild: Guild, event: Event) {
        val messageId = event.recruitMessageId ?: return
        val settings = settingsService.get(guild.idLong)
        val recruitChannel = settings.recruitChannelId?.let { guild.getTextChannelById(it) } ?: return
        recruitChannel.editMessageEmbedsById(messageId, EventEmbeds.recruit(event)).queue({}, {})
    }

    private fun disableRecruitButton(guild: Guild, settings: GuildSettings, event: Event) {
        val messageId = event.recruitMessageId ?: return
        val recruitChannel = settings.recruitChannelId?.let { guild.getTextChannelById(it) } ?: return
        gateway.disableRecruitButton(recruitChannel, messageId, event.id)
    }
}
