package com.github.rain1208.sakenomi.discord.button

import com.github.rain1208.sakenomi.domain.ServiceException
import com.github.rain1208.sakenomi.service.EventService
import com.github.rain1208.sakenomi.service.ParticipantService
import com.github.rain1208.sakenomi.service.SettingsService
import com.github.rain1208.sakenomi.util.Permissions
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

/**
 * ボタンインタラクション(§9.2)
 * - event:join:{eventId}
 * - event:leave:{eventId}
 * - event:delete:confirm:{eventId}
 * - event:delete:cancel:{eventId}
 */
class EventButtonHandler(
    private val eventService: EventService,
    private val participantService: ParticipantService,
    private val settingsService: SettingsService,
) {

    fun handle(event: ButtonInteractionEvent) {
        val parts = event.componentId.split(":")
        if (parts.size < 3 || parts[0] != "event") return

        when (parts[1]) {
            "join" -> join(event, parts[2].toLong())
            "leave" -> leave(event, parts[2].toLong())
            "delete" -> when (parts[2]) {
                "confirm" -> deleteConfirm(event, parts[3].toLong())
                "cancel" -> deleteCancel(event)
            }
        }
    }

    private fun join(event: ButtonInteractionEvent, eventId: Long) {
        event.deferReply(true).queue()
        val joined = participantService.join(event.guild!!, event.member!!, eventId)
        event.hook.sendMessage(
            "✅ **第${joined.number}回 ${joined.name}** に参加しました!\n" +
                "<#${joined.channelId}> でやり取りできます。",
        ).queue()
    }

    private fun leave(event: ButtonInteractionEvent, eventId: Long) {
        event.deferReply(true).queue()
        val left = participantService.leave(event.guild!!, event.member!!, eventId)
        event.hook.sendMessage("✅ **第${left.number}回 ${left.name}** の参加をキャンセルしました").queue()
    }

    private fun deleteConfirm(event: ButtonInteractionEvent, eventId: Long) {
        val guild = event.guild!!
        val settings = settingsService.get(guild.idLong)
        if (!Permissions.isAdmin(event.member!!, settings)) {
            throw ServiceException("イベント削除は運営のみ実行できます")
        }
        val target = eventService.findById(eventId)?.takeIf { it.guildId == guild.idLong }
            ?: throw ServiceException("イベントが見つかりません(既に削除された可能性があります)")

        // 実行元チャンネルごと削除されるため、先に応答を確定させる
        event.editMessage("🗑️ **第${target.number}回 ${target.name}** を削除しました")
            .setComponents()
            .setEmbeds()
            .complete()
        eventService.delete(guild, target, event.member!!)
    }

    private fun deleteCancel(event: ButtonInteractionEvent) {
        event.editMessage("削除をキャンセルしました")
            .setComponents()
            .setEmbeds()
            .queue()
    }
}
