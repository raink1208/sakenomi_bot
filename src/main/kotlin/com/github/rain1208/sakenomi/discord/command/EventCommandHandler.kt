package com.github.rain1208.sakenomi.discord.command

import com.github.rain1208.sakenomi.domain.ServiceException
import com.github.rain1208.sakenomi.service.EventService
import com.github.rain1208.sakenomi.service.GuestService
import com.github.rain1208.sakenomi.service.ParticipantService
import com.github.rain1208.sakenomi.service.SettingsService
import com.github.rain1208.sakenomi.util.EventEmbeds
import com.github.rain1208.sakenomi.util.Permissions
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import java.time.LocalDate
import java.time.format.DateTimeParseException

class EventCommandHandler(
    private val eventService: EventService,
    private val settingsService: SettingsService,
    private val participantService: ParticipantService,
    private val guestService: GuestService,
) {

    fun handle(event: SlashCommandInteractionEvent) {
        when (event.subcommandName) {
            "create" -> create(event)
            "edit" -> edit(event)
            "archive" -> archive(event)
            "reopen" -> reopen(event)
            "transfer-organizer" -> transferOrganizer(event)
            "resend" -> resend(event)
            "delete" -> delete(event)
            "renumber" -> renumber(event)
            "add-user" -> addUser(event)
            "remove-user" -> removeUser(event)
            "add-guest" -> addGuest(event)
            "remove-guest" -> removeGuest(event)
        }
    }

    /** `/event remove-guest` の name 補完。実行チャンネルのイベントのゲスト名を候補に出す */
    fun autoComplete(event: CommandAutoCompleteInteractionEvent) {
        if (event.subcommandName != "remove-guest" || event.focusedOption.name != "name") {
            event.replyChoices(emptyList()).queue({}, {})
            return
        }
        val target = runCatching { eventService.resolveByChannel(event.guild!!, event.channel.idLong) }.getOrNull()
        val query = event.focusedOption.value.trim()
        val names = target?.let { guestService.listActive(it) }.orEmpty()
            .map { it.name }
            .filter { query.isEmpty() || it.contains(query, ignoreCase = true) }
            .take(MAX_AUTOCOMPLETE_CHOICES)
        event.replyChoiceStrings(names).queue({}, {})
    }

    private fun create(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val name = event.getOption("name")!!.asString
        val area = event.getOption("area")!!.asString
        val date = parseDate(event.getOption("date")?.asString)

        // Discord リソース生成に時間がかかるため先に defer する
        event.deferReply(true).queue()
        val created = eventService.create(guild, event.member!!, name, area, date)
        event.hook.sendMessage(
            "✅ **第${created.number}回 ${created.name}** を作成しました!\n" +
                "チャンネル: <#${created.channelId}> / 募集メッセージを募集チャンネルに投稿しました。",
        ).queue()
    }

    private fun edit(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val target = eventService.resolveByChannel(guild, event.channel.idLong)
        val settings = settingsService.get(guild.idLong)
        if (!Permissions.isOrganizerOrAdmin(event.member!!, settings, target)) {
            throw ServiceException("イベント編集は幹事または運営のみ実行できます")
        }

        val name = event.getOption("name")?.asString
        val area = event.getOption("area")?.asString
        val date = parseDate(event.getOption("date")?.asString)
        if (name == null && area == null && date == null) {
            throw ServiceException("変更する項目(`name` / `area` / `date`)を 1 つ以上指定してください")
        }

        val updated = eventService.edit(guild, target, name, area, date)
        event.reply(
            "✅ イベント情報を更新しました。\n" +
                "第${updated.number}回 ${updated.name} / エリア: ${updated.area} / 開催日: ${EventEmbeds.formatDate(updated)}",
        ).setEphemeral(true).queue()
    }

    private fun archive(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val target = eventService.resolveByChannel(guild, event.channel.idLong)
        val settings = settingsService.get(guild.idLong)
        if (!Permissions.isOrganizerOrAdmin(event.member!!, settings, target)) {
            throw ServiceException("アーカイブ化は幹事または運営のみ実行できます")
        }

        event.deferReply(true).queue()
        eventService.archive(guild, target)
        event.hook.sendMessage(
            "✅ **第${target.number}回 ${target.name}** をアーカイブ化しました。" +
                "参加者は閲覧のみ可能になり、新規参加はできなくなります。",
        ).queue()
    }

    private fun reopen(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val target = eventService.resolveByChannel(guild, event.channel.idLong)
        val settings = settingsService.get(guild.idLong)
        if (!Permissions.isOrganizerOrAdmin(event.member!!, settings, target)) {
            throw ServiceException("アーカイブの解除は幹事または運営のみ実行できます")
        }

        event.deferReply(true).queue()
        eventService.reopen(guild, target)
        event.hook.sendMessage(
            "✅ **第${target.number}回 ${target.name}** のアーカイブを解除しました。募集を再開できます。",
        ).queue()
    }

    private fun transferOrganizer(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val target = eventService.resolveByChannel(guild, event.channel.idLong)
        val settings = settingsService.get(guild.idLong)
        if (!Permissions.isOrganizerOrAdmin(event.member!!, settings, target)) {
            throw ServiceException("幹事の変更は幹事または運営のみ実行できます")
        }

        val newOrganizer = event.getOption("user")!!.asUser
        event.deferReply(true).queue()
        eventService.transferOrganizer(guild, target, event.member!!, newOrganizer.idLong)
        event.hook.sendMessage("✅ 幹事を ${newOrganizer.asMention} に変更しました").queue()
    }

    private fun resend(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val target = eventService.resolveByChannel(guild, event.channel.idLong)
        val settings = settingsService.get(guild.idLong)
        if (!Permissions.isOrganizerOrAdmin(event.member!!, settings, target)) {
            throw ServiceException("募集メッセージの再送信は幹事または運営のみ実行できます")
        }

        event.deferReply(true).queue()
        eventService.resend(guild, target)
        event.hook.sendMessage(
            "✅ **第${target.number}回 ${target.name}** の募集メッセージを再送信しました",
        ).queue()
    }

    private fun delete(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val target = eventService.resolveByChannel(guild, event.channel.idLong)
        val settings = settingsService.get(guild.idLong)
        if (!Permissions.isAdmin(event.member!!, settings)) {
            throw ServiceException("イベント削除は運営のみ実行できます")
        }

        val embed = EmbedBuilder()
            .setTitle("⚠️ イベント削除の確認")
            .setDescription(
                "**第${target.number}回 ${target.name}** を削除します。\n" +
                    "チャンネル・ロール・出欠・支払記録がすべて削除されます。この操作は取り消せません。",
            )
            .setColor(0xE74C3C)
            .build()
        event.replyEmbeds(embed)
            .setActionRow(
                Button.danger("event:delete:confirm:${target.id}", "削除する"),
                Button.secondary("event:delete:cancel:${target.id}", "キャンセル"),
            )
            .setEphemeral(true)
            .queue()
    }

    private fun renumber(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val target = eventService.resolveByChannel(guild, event.channel.idLong)
        val settings = settingsService.get(guild.idLong)
        if (!Permissions.isAdmin(event.member!!, settings)) {
            throw ServiceException("採番修正は運営のみ実行できます")
        }

        val newNumber = event.getOption("number")!!.asInt
        val oldNumber = target.number
        val updated = eventService.renumber(guild, target, newNumber, event.member!!)
        event.reply("✅ 第${oldNumber}回 → **第${updated.number}回** に修正しました").setEphemeral(true).queue()
    }

    private fun addUser(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val target = eventService.resolveByChannel(guild, event.channel.idLong)
        val settings = settingsService.get(guild.idLong)
        if (!Permissions.isAdmin(event.member!!, settings)) {
            throw ServiceException("参加者の追加は運営のみ実行できます")
        }

        val user = event.getOption("user")!!.asUser
        val updated = participantService.adminAdd(guild, event.member!!, user.idLong, target.id)
        event.reply(
            "✅ ${user.asMention} を **第${updated.number}回 ${updated.name}** の参加者に追加しました",
        ).setEphemeral(true).queue()
    }

    private fun removeUser(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val target = eventService.resolveByChannel(guild, event.channel.idLong)
        val settings = settingsService.get(guild.idLong)
        if (!Permissions.isAdmin(event.member!!, settings)) {
            throw ServiceException("参加者の削除は運営のみ実行できます")
        }

        val user = event.getOption("user")!!.asUser
        val updated = participantService.adminRemove(guild, event.member!!, user.idLong, target.id)
        event.reply(
            "✅ ${user.asMention} を **第${updated.number}回 ${updated.name}** の参加者から削除しました",
        ).setEphemeral(true).queue()
    }

    private fun addGuest(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val target = eventService.resolveByChannel(guild, event.channel.idLong)
        val settings = settingsService.get(guild.idLong)
        if (!Permissions.isOrganizerOrAdmin(event.member!!, settings, target)) {
            throw ServiceException("ゲストの追加は幹事または運営のみ実行できます")
        }

        val name = event.getOption("name")!!.asString
        val result = guestService.add(guild, event.member!!, target, name)
        val restoredNote = if (result.restored) "(以前の出欠・受領記録を復元しました)" else ""
        event.reply(
            "✅ **${result.guest.displayName}** を **第${target.number}回 ${target.name}** に追加しました$restoredNote",
        ).setEphemeral(true).queue()
    }

    private fun removeGuest(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val target = eventService.resolveByChannel(guild, event.channel.idLong)
        val settings = settingsService.get(guild.idLong)
        if (!Permissions.isOrganizerOrAdmin(event.member!!, settings, target)) {
            throw ServiceException("ゲストの削除は幹事または運営のみ実行できます")
        }

        val guest = guestService.findActiveByName(target, event.getOption("name")!!.asString)
        guestService.remove(guild, event.member!!, target, guest)
        event.reply(
            "✅ **${guest.displayName}** を **第${target.number}回 ${target.name}** から削除しました\n" +
                "出欠・受領記録は残っており、同じ名前で再追加すると元に戻ります",
        ).setEphemeral(true).queue()
    }

    private fun parseDate(value: String?): LocalDate? {
        if (value == null) return null
        return try {
            LocalDate.parse(value)
        } catch (e: DateTimeParseException) {
            throw ServiceException("日付は `YYYY-MM-DD` 形式で指定してください(例: 2026-08-01)")
        }
    }

    companion object {
        /** Discord のオートコンプリート候補の上限 */
        private const val MAX_AUTOCOMPLETE_CHOICES = 25
    }
}
