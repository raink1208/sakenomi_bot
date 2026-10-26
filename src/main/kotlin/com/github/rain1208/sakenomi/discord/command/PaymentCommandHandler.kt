package com.github.rain1208.sakenomi.discord.command

import com.github.rain1208.sakenomi.domain.ServiceException
import com.github.rain1208.sakenomi.service.EventService
import com.github.rain1208.sakenomi.service.PaymentService
import com.github.rain1208.sakenomi.service.RosterService
import com.github.rain1208.sakenomi.service.SettingsService
import com.github.rain1208.sakenomi.util.EventEmbeds
import com.github.rain1208.sakenomi.util.Permissions
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class PaymentCommandHandler(
    private val eventService: EventService,
    private val paymentService: PaymentService,
    private val settingsService: SettingsService,
    private val rosterService: RosterService,
) {

    fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val target = eventService.resolveByChannel(guild, event.channel.idLong)
        val settings = settingsService.get(guild.idLong)
        if (!Permissions.isOrganizerOrAdmin(event.member!!, settings, target)) {
            throw ServiceException("支払管理は幹事または運営のみ実行できます")
        }

        when (event.subcommandName) {
            "add" -> add(event)
            "list" -> list(event)
        }
    }

    private fun add(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val target = eventService.resolveByChannel(guild, event.channel.idLong)
        val orderNo = event.getOption("party")!!.asInt
        val payer = event.getOption("payer")!!.asUser
        val amount = event.getOption("amount")!!.asLong

        val result = paymentService.add(target, orderNo, payer.idLong, amount, event.user.idLong)
        event.reply(
            "✅ **${result.party.orderNo}軒目: ${result.party.shopName}** に " +
                "${payer.asMention} から ${EventEmbeds.formatYen(amount)} の受領を記録しました。\n" +
                "受領合計: ${EventEmbeds.formatYen(result.total)}",
        ).setEphemeral(true).queue()
    }

    private fun list(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val target = eventService.resolveByChannel(guild, event.channel.idLong)
        val orderNo = event.getOption("party")?.asInt

        val results = paymentService.list(target, orderNo)
        if (results.isEmpty()) {
            event.reply("まだお店が登録されていません。`/party add` で登録してください").setEphemeral(true).queue()
            return
        }

        val guestLabels = rosterService.guestLabels(target.id)
        val builder = EmbedBuilder()
            .setTitle("💰 第${target.number}回 ${target.name} の受領記録")
            .setColor(0xF39C12)
        results.forEach { result ->
            val lines = buildList {
                if (result.payments.isEmpty()) {
                    add("受領記録なし")
                } else {
                    result.payments.forEach { payment ->
                        add("${guestLabels[payment.payerUserId] ?: "<@${payment.payerUserId}>"} ${EventEmbeds.formatYen(payment.amount)}")
                    }
                }
                add("**受領合計: ${EventEmbeds.formatYen(result.total)}**")
                result.party.totalAmount?.let { total ->
                    add("会計金額: ${EventEmbeds.formatYen(total)}")
                }
            }
            builder.addField(
                "${result.party.orderNo}軒目: ${result.party.shopName}",
                lines.joinToString("\n"),
                false,
            )
        }
        event.replyEmbeds(builder.build()).setEphemeral(true).queue()
    }
}
