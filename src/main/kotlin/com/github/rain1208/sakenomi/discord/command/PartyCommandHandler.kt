package com.github.rain1208.sakenomi.discord.command

import com.github.rain1208.sakenomi.domain.ServiceException
import com.github.rain1208.sakenomi.service.EventService
import com.github.rain1208.sakenomi.service.ParticipantService
import com.github.rain1208.sakenomi.service.PartyService
import com.github.rain1208.sakenomi.service.SettingsService
import com.github.rain1208.sakenomi.util.EventEmbeds
import com.github.rain1208.sakenomi.util.Permissions
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class PartyCommandHandler(
    private val eventService: EventService,
    private val partyService: PartyService,
    private val participantService: ParticipantService,
    private val settingsService: SettingsService,
) {

    fun handle(event: SlashCommandInteractionEvent) {
        when (event.subcommandName) {
            "add" -> add(event)
            "list" -> list(event)
        }
    }

    private fun add(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val target = eventService.resolveByChannel(guild, event.channel.idLong)
        val settings = settingsService.get(guild.idLong)
        if (!Permissions.isOrganizerOrAdmin(event.member!!, settings, target)) {
            throw ServiceException("お店の登録は幹事または運営のみ実行できます")
        }

        val shop = event.getOption("shop")!!.asString
        val order = event.getOption("order")?.asInt
        val amount = event.getOption("amount")?.asLong
        val note = event.getOption("note")?.asString

        val party = partyService.add(target, shop, order, amount, note)
        val amountText = party.totalAmount?.let { " / 会計: ${EventEmbeds.formatYen(it)}" } ?: ""
        event.reply("✅ **${party.orderNo}軒目: ${party.shopName}** を登録しました$amountText")
            .setEphemeral(true)
            .queue()
    }

    private fun list(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val target = eventService.resolveByChannel(guild, event.channel.idLong)
        val settings = settingsService.get(guild.idLong)
        val member = event.member!!
        val allowed = Permissions.isOrganizerOrAdmin(member, settings, target) ||
            participantService.isParticipant(target.id, member.idLong)
        if (!allowed) {
            throw ServiceException("お店一覧は幹事・運営・参加者のみ閲覧できます")
        }

        val parties = partyService.list(target)
        if (parties.isEmpty()) {
            event.reply("まだお店が登録されていません。`/party add` で登録してください").setEphemeral(true).queue()
            return
        }

        val builder = EmbedBuilder()
            .setTitle("🏮 第${target.number}回 ${target.name} のお店一覧")
            .setColor(0x1ABC9C)
        parties.forEach { party ->
            val lines = buildList {
                add(party.totalAmount?.let { "会計: ${EventEmbeds.formatYen(it)}" } ?: "会計: 未確定")
                party.note?.let { add("メモ: $it") }
            }
            builder.addField("${party.orderNo}軒目: ${party.shopName}", lines.joinToString("\n"), false)
        }
        event.replyEmbeds(builder.build()).setEphemeral(true).queue()
    }
}
