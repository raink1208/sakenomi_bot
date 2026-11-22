package com.github.rain1208.sakenomi.web.routes

import com.github.rain1208.sakenomi.discord.DiscordGateway
import com.github.rain1208.sakenomi.domain.EventStatus
import com.github.rain1208.sakenomi.repository.PaymentRepository
import com.github.rain1208.sakenomi.service.EventService
import com.github.rain1208.sakenomi.service.ParticipantService
import com.github.rain1208.sakenomi.service.PartyService
import com.github.rain1208.sakenomi.service.SettingsService
import com.github.rain1208.sakenomi.util.EventEmbeds
import com.github.rain1208.sakenomi.web.auth.requireGuildContext
import com.github.rain1208.sakenomi.web.viewmodel.ViewModels
import io.ktor.server.freemarker.FreeMarkerContent
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DISPLAY_ZONE: ZoneId = ZoneId.of("Asia/Tokyo")
private val RECEIVED_AT_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")

/** マイページ(Web仕様書.md §5.5): 自分の参加履歴・支払記録 */
fun Route.myPageRoutes(
    gateway: DiscordGateway,
    settingsService: SettingsService,
    eventService: EventService,
    participantService: ParticipantService,
    partyService: PartyService,
    paymentRepository: PaymentRepository,
) {
    get("/g/{guildId}/me") {
        val ctx = requireGuildContext(gateway, settingsService) ?: return@get

        val myEvents = participantService.listEventIdsByUser(ctx.session.userId)
            .mapNotNull { eventService.findById(it) }
            .filter { it.guildId == ctx.guild.idLong }
            .sortedByDescending { it.number }

        val partyByEvent = myEvents.associateWith { partyService.list(it) }
        val partyIdToEvent = partyByEvent.entries.flatMap { (event, parties) -> parties.map { it.id to event } }.toMap()
        val partyIdToShop = partyByEvent.values.flatten().associateBy({ it.id }, { it.shopName })
        val allPartyIds = partyByEvent.values.flatten().map { it.id }

        val myPayments = paymentRepository.listByPayerAndParties(ctx.session.userId, allPartyIds)
        val total = myPayments.sumOf { it.amount }

        val paymentRows = myPayments.map { payment ->
            val event = partyIdToEvent[payment.partyId]
            mapOf(
                "eventName" to (event?.let { "第${it.number}回 ${it.name}" } ?: "不明なイベント"),
                "shopName" to (partyIdToShop[payment.partyId] ?: "不明な店舗"),
                "amount" to payment.amount,
                "amountText" to EventEmbeds.formatYen(payment.amount),
                "receivedAt" to payment.receivedAt.atZoneSameInstant(DISPLAY_ZONE).format(RECEIVED_AT_FORMAT),
            )
        }

        // 次の参加予定: 募集中かつ開催日が今日以降のうち、最も近いもの
        val today = LocalDate.now(DISPLAY_ZONE)
        val nextEvent = myEvents
            .filter { it.status == EventStatus.RECRUITING && it.eventDate != null && !it.eventDate.isBefore(today) }
            .minByOrNull { it.eventDate!! }

        call.respond(
            FreeMarkerContent(
                "mypage.ftl",
                mapOf(
                    "session" to ctx.session,
                    "guildId" to ctx.guild.idLong,
                    "guildName" to ctx.guild.name,
                    "isAdmin" to ctx.isAdmin,
                    "events" to myEvents.map { ViewModels.event(it, participantCount = null) },
                    "nextEvent" to nextEvent?.let { ViewModels.event(it, participantCount = null) },
                    "payments" to paymentRows,
                    "total" to total,
                    "totalText" to EventEmbeds.formatYen(total),
                ),
            ),
        )
    }
}
