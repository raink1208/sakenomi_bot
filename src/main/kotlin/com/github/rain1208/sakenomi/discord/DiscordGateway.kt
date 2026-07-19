package com.github.rain1208.sakenomi.discord

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.TimeoutException

/** チャンネル / ロール / 権限の生成・変更・削除の抽象化 */
class DiscordGateway {

    private val logger = LoggerFactory.getLogger(javaClass)

    private lateinit var jda: JDA

    /** JDA の ready 後に呼び出す(Web コンソールが Discord 情報を参照するため) */
    fun attachJda(jda: JDA) {
        this.jda = jda
    }

    fun findGuild(guildId: Long): Guild? = jda.getGuildById(guildId)

    /** Web コンソールのギルドメンバーシップ確認・admin-role 判定用 */
    fun findMember(guildId: Long, userId: Long): Member? {
        val guild = findGuild(guildId) ?: return null
        guild.getMemberById(userId)?.let { return it }
        return try {
            guild.retrieveMemberById(userId).complete()
        } catch (e: ErrorResponseException) {
            null
        }
    }

    /** Bot 導入済み ∩ ユーザー所属ギルドの一覧(ギルド選択画面用) */
    fun listMutualGuilds(userId: Long): List<Guild> =
        jda.guilds.filter { guild -> findMember(guild.idLong, userId) != null }

    /** 参加者一覧・受領記録の表示名解決 */
    fun resolveDisplayNames(guildId: Long, userIds: Collection<Long>): Map<Long, String> {
        val guild = findGuild(guildId) ?: return userIds.associateWith { "不明なユーザー($it)" }
        return userIds.associateWith { userId ->
            findMember(guildId, userId)?.effectiveName ?: "不明なユーザー($userId)"
        }
    }

    /**
     * Web コンソールの参加者追加用プルダウン(GUILD_MEMBERS 特権インテントが必要)。
     * メンバーチャンク取得はデフォルト 10 秒でタイムアウトするため、大規模ギルドや
     * 混雑時に失敗しても他の機能に影響しないよう空リストにフォールバックする
     */
    fun listMembers(guildId: Long): List<Member> {
        val guild = findGuild(guildId) ?: return emptyList()
        return try {
            guild.loadMembers().setTimeout(Duration.ofSeconds(30)).get()
        } catch (e: Exception) {
            if (e is TimeoutException || e.cause is TimeoutException) {
                logger.warn("メンバー一覧の取得がタイムアウトしました (guildId={})", guildId)
            } else {
                logger.warn("メンバー一覧の取得に失敗しました (guildId={})", guildId, e)
            }
            emptyList()
        }
    }

    fun listCategories(guildId: Long): List<Category> = findGuild(guildId)?.categories ?: emptyList()

    fun listRoles(guildId: Long): List<Role> = findGuild(guildId)?.roles ?: emptyList()

    fun listTextChannels(guildId: Long): List<TextChannel> = findGuild(guildId)?.textChannels ?: emptyList()

    fun createEventRole(guild: Guild, number: Int): Role =
        guild.createRole()
            .setName("第${number}回参加者")
            .setMentionable(true)
            .complete()

    /**
     * イベントチャンネルを作成する。
     * 権限: @everyone 閲覧不可 / 参加者ロール 閲覧・送信可 / 運営ロール 閲覧・送信可
     * Bot 自身にも明示的に閲覧・管理権限を付与する(カテゴリ側の権限設定に依存すると、
     * カテゴリが @everyone の閲覧を拒否している場合に Bot 自身が作成済みチャンネルへ
     * アクセスできなくなり、以後の操作や補償削除が MissingAccessException で失敗するため)
     */
    fun createEventChannel(
        guild: Guild,
        category: Category,
        channelName: String,
        participantRole: Role,
        adminRole: Role?,
    ): TextChannel {
        var action = guild.createTextChannel(channelName, category)
            .addPermissionOverride(guild.publicRole, emptyList(), listOf(Permission.VIEW_CHANNEL))
            .addPermissionOverride(
                guild.selfMember,
                listOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MANAGE_CHANNEL),
                emptyList(),
            )
            .addPermissionOverride(
                participantRole,
                listOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND),
                emptyList(),
            )
        if (adminRole != null) {
            action = action.addPermissionOverride(
                adminRole,
                listOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND),
                emptyList(),
            )
        }
        return action.complete()
    }

    fun countChannels(category: Category): Int = category.channels.size

    fun moveToCategory(channel: TextChannel, category: Category) {
        channel.manager.setParent(category).complete()
    }

    /** 対象ロールの送信権限を剥奪し閲覧のみとする(アーカイブ用) */
    fun revokeSendPermission(channel: TextChannel, roles: List<Role>) {
        roles.forEach { role ->
            channel.upsertPermissionOverride(role)
                .grant(Permission.VIEW_CHANNEL)
                .deny(Permission.MESSAGE_SEND)
                .complete()
        }
    }

    fun renameChannel(channel: TextChannel, name: String) {
        channel.manager.setName(name).queue()
    }

    fun renameRole(role: Role, name: String) {
        role.manager.setName(name).queue()
    }

    fun grantRole(guild: Guild, member: Member, roleId: Long) {
        val role = guild.getRoleById(roleId) ?: return
        guild.addRoleToMember(member, role).complete()
    }

    fun removeRole(guild: Guild, member: Member, roleId: Long) {
        val role = guild.getRoleById(roleId) ?: return
        guild.removeRoleFromMember(member, role).complete()
    }

    /** 募集メッセージの参加ボタンを無効化する */
    fun disableRecruitButton(channel: TextChannel, messageId: Long, eventId: Long) {
        channel.editMessageComponentsById(
            messageId,
            ActionRow.of(Button.success("event:join:$eventId", "参加する").asDisabled()),
        ).queue({}, {})
    }

    fun deleteMessage(channel: TextChannel, messageId: Long) {
        channel.deleteMessageById(messageId).queue({}, {})
    }

    /** イベントチャンネルへの参加/退出ログ投稿など */
    fun sendChannelMessage(guild: Guild, channelId: Long, content: String) {
        guild.getTextChannelById(channelId)?.sendMessage(content)?.queue({}, {})
    }

    fun deleteRole(guild: Guild, roleId: Long) {
        guild.getRoleById(roleId)?.delete()?.queue({}, {})
    }

    fun deleteChannel(guild: Guild, channelId: Long) {
        guild.getTextChannelById(channelId)?.delete()?.queue({}, {})
    }
}
