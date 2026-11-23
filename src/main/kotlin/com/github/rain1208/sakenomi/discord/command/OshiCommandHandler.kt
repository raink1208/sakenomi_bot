package com.github.rain1208.sakenomi.discord.command

import com.github.rain1208.sakenomi.discord.component.OshiComponentHandler
import com.github.rain1208.sakenomi.domain.ServiceException
import com.github.rain1208.sakenomi.service.OshiService
import com.github.rain1208.sakenomi.service.SettingsService
import com.github.rain1208.sakenomi.util.Permissions
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.exceptions.PermissionException
import net.dv8tion.jda.api.interactions.components.ActionRow

/**
 * 推しロール管理(運営のみ)
 * - /oshi add role: name:  … 手動作成したロールを推しとして登録
 * - /oshi remove name:     … 登録解除(ロール自体は削除しない)
 * - /oshi list             … 登録一覧
 * - /oshi panel            … 実行したチャンネルに推し選択パネルを設置
 */
class OshiCommandHandler(
    private val oshiService: OshiService,
    private val settingsService: SettingsService,
) {

    fun handle(event: SlashCommandInteractionEvent) {
        val settings = settingsService.get(event.guild!!.idLong)
        if (!Permissions.isAdmin(event.member!!, settings)) {
            throw ServiceException("このコマンドは運営ロールまたはサーバー管理権限が必要です")
        }
        when (event.subcommandName) {
            "add" -> add(event)
            "remove" -> remove(event)
            "list" -> list(event)
            "panel" -> panel(event)
        }
    }

    fun autoComplete(event: CommandAutoCompleteInteractionEvent) {
        if (event.subcommandName != "remove" || event.focusedOption.name != "name") {
            event.replyChoices(emptyList()).queue({}, {})
            return
        }
        val query = event.focusedOption.value.trim()
        val names = oshiService.list(event.guild!!.idLong)
            .map { it.name }
            .filter { query.isEmpty() || it.contains(query, ignoreCase = true) }
            .take(MAX_AUTOCOMPLETE_CHOICES)
        event.replyChoiceStrings(names).queue({}, {})
    }

    private fun add(event: SlashCommandInteractionEvent) {
        val role = event.getOption("role")!!.asRole
        val name = event.getOption("name")!!.asString
        val oshi = oshiService.register(event.guild!!, role, name)
        event.reply("✅ 推し「${oshi.name}」(${role.asMention})を登録しました").setEphemeral(true).queue()
    }

    private fun remove(event: SlashCommandInteractionEvent) {
        val oshi = oshiService.unregister(event.guild!!.idLong, event.getOption("name")!!.asString)
        event.reply(
            "✅ 推し「${oshi.name}」の登録を解除しました。\n" +
                "Discord 上のロール(<@&${oshi.roleId}>)とメンバーへの付与状態はそのまま残ります。不要であれば手動で削除してください",
        ).setEphemeral(true).queue()
    }

    private fun list(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val oshiList = oshiService.list(guild.idLong)
        if (oshiList.isEmpty()) {
            event.reply("推しはまだ登録されていません。`/oshi add` で登録してください").setEphemeral(true).queue()
            return
        }
        // パネルの選択肢と同じく Discord のロール順。ロールが削除された推しは末尾にまとめる
        val available = oshiService.listAvailable(guild)
        val availableIds = available.map { (oshi, _) -> oshi.id }.toSet()
        val missing = oshiList.filter { it.id !in availableIds }
        val lines = available.map { (oshi, role) ->
            "・${oshi.name} — ${role.asMention}(${guild.getMembersWithRoles(role).size}人)"
        } + missing.map { oshi ->
            "・${oshi.name} — ⚠️ ロールが削除されています(`/oshi remove` で登録解除してください)"
        }
        val embed = EmbedBuilder()
            .setTitle("💖 推し一覧(${oshiList.size}件)")
            .setDescription(lines.joinToString("\n").take(MAX_EMBED_DESCRIPTION))
            .setColor(OshiComponentHandler.COLOR)
            .build()
        event.replyEmbeds(embed).setEphemeral(true).queue()
    }

    private fun panel(event: SlashCommandInteractionEvent) {
        try {
            event.channel.sendMessageEmbeds(OshiComponentHandler.panelEmbed())
                .setComponents(ActionRow.of(OshiComponentHandler.openButton()))
                .complete()
        } catch (e: PermissionException) {
            throw ServiceException("このチャンネルにメッセージを送信する権限が Bot にありません", e)
        }
        event.reply("✅ このチャンネルに推し選択パネルを設置しました").setEphemeral(true).queue()
    }

    private companion object {
        const val MAX_AUTOCOMPLETE_CHOICES = 25
        const val MAX_EMBED_DESCRIPTION = 4096
    }
}
