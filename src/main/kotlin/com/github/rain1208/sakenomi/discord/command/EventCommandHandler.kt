package com.github.rain1208.sakenomi.discord.command

import com.github.rain1208.sakenomi.domain.ServiceException
import com.github.rain1208.sakenomi.service.EventService
import com.github.rain1208.sakenomi.service.ParticipantService
import com.github.rain1208.sakenomi.service.SettingsService
import com.github.rain1208.sakenomi.util.EventEmbeds
import com.github.rain1208.sakenomi.util.Permissions
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import java.time.LocalDate
import java.time.format.DateTimeParseException

class EventCommandHandler(
    private val eventService: EventService,
    private val settingsService: SettingsService,
    private val participantService: ParticipantService,
) {

    fun handle(event: SlashCommandInteractionEvent) {
        when (event.subcommandName) {
            "create" -> create(event)
            "edit" -> edit(event)
            "archive" -> archive(event)
            "delete" -> delete(event)
            "renumber" -> renumber(event)
            "add-user" -> addUser(event)
            "remove-user" -> removeUser(event)
        }
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

    private fun parseDate(value: String?): LocalDate? {
        if (value == null) return null
        return try {
            LocalDate.parse(value)
        } catch (e: DateTimeParseException) {
            throw ServiceException("日付は `YYYY-MM-DD` 形式で指定してください(例: 2026-08-01)")
        }
    }
}
