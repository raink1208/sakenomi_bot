package com.github.rain1208.sakenomi.discord.command

import com.github.rain1208.sakenomi.domain.AttendanceStatus
import com.github.rain1208.sakenomi.domain.ServiceException
import com.github.rain1208.sakenomi.service.AttendanceService
import com.github.rain1208.sakenomi.service.EventService
import com.github.rain1208.sakenomi.service.ParticipantService
import com.github.rain1208.sakenomi.service.SettingsService
import com.github.rain1208.sakenomi.util.Permissions
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class AttendanceCommandHandler(
    private val eventService: EventService,
    private val attendanceService: AttendanceService,
    private val participantService: ParticipantService,
    private val settingsService: SettingsService,
) {

    fun handle(event: SlashCommandInteractionEvent) {
        when (event.subcommandName) {
            "set" -> set(event)
            "list" -> list(event)
        }
    }

    private fun set(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val target = eventService.resolveByChannel(guild, event.channel.idLong)
        val settings = settingsService.get(guild.idLong)
        if (!Permissions.isOrganizerOrAdmin(event.member!!, settings, target)) {
            throw ServiceException("出欠の記録は幹事または運営のみ実行できます")
        }

        val orderNo = event.getOption("party")!!.asInt
        val user = event.getOption("user")!!.asUser
        val status = AttendanceStatus.valueOf(event.getOption("status")!!.asString)

        val result = attendanceService.set(target, orderNo, user.idLong, status, event.user.idLong)
        val statusText = if (status == AttendanceStatus.PRESENT) "✅ 出席" else "❌ 欠席"
        event.reply(
            "$statusText として記録しました: **${result.party.orderNo}軒目: ${result.party.shopName}** / ${user.asMention}",
        ).setEphemeral(true).queue()
    }

    private fun list(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val target = eventService.resolveByChannel(guild, event.channel.idLong)
        val settings = settingsService.get(guild.idLong)
        val member = event.member!!
        val allowed = Permissions.isOrganizerOrAdmin(member, settings, target) ||
            participantService.isParticipant(target.id, member.idLong)
        if (!allowed) {
            throw ServiceException("出欠一覧は幹事・運営・参加者のみ閲覧できます")
        }

        val orderNo = event.getOption("party")?.asInt
        val results = attendanceService.list(target, orderNo)
        if (results.isEmpty()) {
            event.reply("まだお店が登録されていません。`/party add` で登録してください").setEphemeral(true).queue()
            return
        }

        val builder = EmbedBuilder()
            .setTitle("📋 第${target.number}回 ${target.name} の出欠")
            .setColor(0x3498DB)
        results.forEach { result ->
            val lines = result.entries.map { entry ->
                val mark = when (entry.status) {
                    AttendanceStatus.PRESENT -> "✅"
                    AttendanceStatus.ABSENT -> "❌"
                    null -> "❔"
                }
                "$mark <@${entry.userId}>"
            }
            val summary = "出席 ${result.presentCount} / 欠席 ${result.absentCount} / 未確認 ${result.unconfirmedCount}"
            builder.addField(
                "${result.party.orderNo}軒目: ${result.party.shopName}",
                (lines + "**$summary**").joinToString("\n"),
                false,
            )
        }
        event.replyEmbeds(builder.build()).setEphemeral(true).queue()
    }
}
