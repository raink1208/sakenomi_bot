package com.github.rain1208.sakenomi.web.routes

import com.github.rain1208.sakenomi.discord.DiscordGateway
import com.github.rain1208.sakenomi.domain.AttendanceStatus
import com.github.rain1208.sakenomi.domain.ParticipantIds
import com.github.rain1208.sakenomi.domain.ServiceException
import com.github.rain1208.sakenomi.repository.PartyRepository
import com.github.rain1208.sakenomi.service.AttendanceEntry
import com.github.rain1208.sakenomi.service.AttendanceService
import com.github.rain1208.sakenomi.service.EventService
import com.github.rain1208.sakenomi.service.GuestService
import com.github.rain1208.sakenomi.service.PartyService
import com.github.rain1208.sakenomi.service.PaymentService
import com.github.rain1208.sakenomi.service.RosterService
import com.github.rain1208.sakenomi.service.SettingsService
import com.github.rain1208.sakenomi.util.Permissions
import com.github.rain1208.sakenomi.web.auth.requireGuildContext
import com.github.rain1208.sakenomi.web.viewmodel.ViewModels
import io.ktor.http.HttpStatusCode
import io.ktor.server.freemarker.FreeMarkerContent
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.text.Collator
import java.util.Locale

/** 飲み会・支払・出欠ルーティング(Web仕様書.md §5.4) */
fun Route.partyPaymentRoutes(
    gateway: DiscordGateway,
    settingsService: SettingsService,
    eventService: EventService,
    partyService: PartyService,
    paymentService: PaymentService,
    attendanceService: AttendanceService,
    partyRepository: PartyRepository,
    rosterService: RosterService,
    guestService: GuestService,
) {
    route("/g/{guildId}/parties/{partyId}") {
        post("/edit") {
            val ctx = requireGuildContext(gateway, settingsService) ?: return@post
            val partyId = call.parameters["partyId"]?.toLongOrNull()
            val party = partyId?.let { partyRepository.findById(it) }
            val event = party?.let { eventService.findById(it.eventId) }
            if (party == null || event == null || event.guildId != ctx.guild.idLong) {
                call.respondText("お店情報が見つかりません", status = HttpStatusCode.NotFound)
                return@post
            }
            if (!Permissions.isOrganizerOrAdmin(ctx.member, ctx.settings, event)) {
                call.respondText("編集権限がありません", status = HttpStatusCode.Forbidden)
                return@post
            }
            val params = call.receiveParameters()
            val shopName = params["shopName"]?.trim().orEmpty()
            if (shopName.isBlank()) throw ServiceException("店名は必須です")
            val totalAmount = params["totalAmount"]?.takeIf { it.isNotBlank() }?.toLongOrNull()
            if (totalAmount != null && totalAmount < 0) throw ServiceException("会計金額は 0 以上で指定してください")
            val note = params["note"]?.trim()?.takeIf { it.isNotBlank() }
            partyRepository.updateInfo(party.id, shopName, totalAmount, note)
            call.respondRedirect("/g/${ctx.guild.idLong}/events/${event.id}")
        }

        post("/delete") {
            val ctx = requireGuildContext(gateway, settingsService) ?: return@post
            val partyId = call.parameters["partyId"]?.toLongOrNull()
            val party = partyId?.let { partyRepository.findById(it) }
            val event = party?.let { eventService.findById(it.eventId) }
            if (party == null || event == null || event.guildId != ctx.guild.idLong) {
                call.respondText("お店情報が見つかりません", status = HttpStatusCode.NotFound)
                return@post
            }
            if (!Permissions.isOrganizerOrAdmin(ctx.member, ctx.settings, event)) {
                call.respondText("削除権限がありません", status = HttpStatusCode.Forbidden)
                return@post
            }
            partyRepository.delete(party.id)
            call.respondRedirect("/g/${ctx.guild.idLong}/events/${event.id}")
        }

        post("/payments") {
            val ctx = requireGuildContext(gateway, settingsService) ?: return@post
            val partyId = call.parameters["partyId"]?.toLongOrNull()
            val party = partyId?.let { partyRepository.findById(it) }
            val event = party?.let { eventService.findById(it.eventId) }
            if (party == null || event == null || event.guildId != ctx.guild.idLong) {
                call.respondText("お店情報が見つかりません", status = HttpStatusCode.NotFound)
                return@post
            }
            if (!Permissions.isOrganizerOrAdmin(ctx.member, ctx.settings, event)) {
                call.respondText("受領記録の登録権限がありません", status = HttpStatusCode.Forbidden)
                return@post
            }
            val params = call.receiveParameters()
            val payerUserId = params["payerUserId"]?.toLongOrNull()
                ?: throw ServiceException("支払者を選択してください")
            val amount = params["amount"]?.toLongOrNull()
                ?: throw ServiceException("金額を指定してください")
            paymentService.add(event, party.orderNo, payerUserId, amount, ctx.session.userId)
            call.respondRedirect("/g/${ctx.guild.idLong}/events/${event.id}")
        }

        post("/attendance") {
            val ctx = requireGuildContext(gateway, settingsService) ?: return@post
            val partyId = call.parameters["partyId"]?.toLongOrNull()
            val party = partyId?.let { partyRepository.findById(it) }
            val event = party?.let { eventService.findById(it.eventId) }
            if (party == null || event == null || event.guildId != ctx.guild.idLong) {
                call.respondText("お店情報が見つかりません", status = HttpStatusCode.NotFound)
                return@post
            }
            if (!Permissions.isOrganizerOrAdmin(ctx.member, ctx.settings, event)) {
                call.respondText("出欠の記録権限がありません", status = HttpStatusCode.Forbidden)
                return@post
            }
            val params = call.receiveParameters()
            val userId = params["userId"]?.toLongOrNull() ?: throw ServiceException("対象の参加者を選択してください")
            when (val statusParam = params["status"]) {
                "CLEAR" -> attendanceService.clear(event, party.orderNo, userId)
                else -> {
                    val status = AttendanceStatus.entries.find { it.name == statusParam }
                        ?: throw ServiceException("出欠状態を指定してください")
                    attendanceService.set(event, party.orderNo, userId, status, ctx.session.userId)
                }
            }
            call.respondRedirect("/g/${ctx.guild.idLong}/events/${event.id}")
        }

        // 人数が多いイベント向けの出席登録専用ページ。出席 / 未登録の2状態だけを扱い、受領金額はイベント詳細で登録する
        get("/check-in") {
            val ctx = requireGuildContext(gateway, settingsService) ?: return@get
            val partyId = call.parameters["partyId"]?.toLongOrNull()
            val party = partyId?.let { partyRepository.findById(it) }
            val event = party?.let { eventService.findById(it.eventId) }
            if (party == null || event == null || event.guildId != ctx.guild.idLong) {
                call.respondText("お店情報が見つかりません", status = HttpStatusCode.NotFound)
                return@get
            }
            if (!Permissions.isOrganizerOrAdmin(ctx.member, ctx.settings, event)) {
                call.respondText("出席登録の権限がありません", status = HttpStatusCode.Forbidden)
                return@get
            }

            val entries = attendanceService.list(event, party.orderNo).single().entries
            val guestNames = guestService.listActive(event).associate { it.participantId to it.name }
            val memberNames = rosterService.resolveDisplayNames(
                ctx.guild.idLong,
                event.id,
                entries.map { it.userId }.filterNot { ParticipantIds.isGuest(it) },
            )
            val nameOf = { userId: Long -> guestNames[userId] ?: memberNames[userId] ?: userId.toString() }
            // 人数が多くても探しやすいよう、幹事 → メンバー → ゲストの順に、それぞれ名前順で並べる
            val collator = Collator.getInstance(Locale.JAPANESE)
            val people = entries
                .sortedWith(
                    compareBy<AttendanceEntry>({ it.userId != event.organizerUserId }, { ParticipantIds.isGuest(it.userId) })
                        .thenBy(collator) { nameOf(it.userId) },
                )
                .map { entry ->
                    mapOf(
                        "id" to entry.userId,
                        "name" to nameOf(entry.userId),
                        "isOrganizer" to (entry.userId == event.organizerUserId),
                        "isGuest" to ParticipantIds.isGuest(entry.userId),
                        "present" to (entry.status == AttendanceStatus.PRESENT),
                    )
                }

            call.respond(
                FreeMarkerContent(
                    "party-check-in.ftl",
                    mapOf(
                        "session" to ctx.session,
                        "guildId" to ctx.guild.idLong,
                        "isAdmin" to ctx.isAdmin,
                        "event" to ViewModels.event(event),
                        "party" to ViewModels.party(party),
                        "parties" to partyService.list(event).map { ViewModels.party(it) },
                        "people" to people,
                        "presentCount" to entries.count { it.status == AttendanceStatus.PRESENT },
                        "saved" to (call.request.queryParameters["saved"] != null),
                    ),
                ),
            )
        }

        post("/check-in") {
            val ctx = requireGuildContext(gateway, settingsService) ?: return@post
            val partyId = call.parameters["partyId"]?.toLongOrNull()
            val party = partyId?.let { partyRepository.findById(it) }
            val event = party?.let { eventService.findById(it.eventId) }
            if (party == null || event == null || event.guildId != ctx.guild.idLong) {
                call.respondText("お店情報が見つかりません", status = HttpStatusCode.NotFound)
                return@post
            }
            if (!Permissions.isOrganizerOrAdmin(ctx.member, ctx.settings, event)) {
                call.respondText("出席登録の権限がありません", status = HttpStatusCode.Forbidden)
                return@post
            }
            val params = call.receiveParameters()
            val before = params.getAll("wasPresent").orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
            val after = params.getAll("present").orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
            // 画面を開いてから変更した人だけを反映する。触っていない人の欠席記録や、
            // 他の幹事が同時に付けた出欠を一括で上書きしないため
            val statuses = (after - before).associateWith { "PRESENT" } + (before - after).associateWith { "CLEAR" }
            attendanceService.setAll(event, party.orderNo, statuses, ctx.session.userId)
            call.respondRedirect("/g/${ctx.guild.idLong}/parties/${party.id}/check-in?saved=1")
        }

        post("/attendance-payments") {
            val ctx = requireGuildContext(gateway, settingsService) ?: return@post
            val partyId = call.parameters["partyId"]?.toLongOrNull()
            val party = partyId?.let { partyRepository.findById(it) }
            val event = party?.let { eventService.findById(it.eventId) }
            if (party == null || event == null || event.guildId != ctx.guild.idLong) {
                call.respondText("お店情報が見つかりません", status = HttpStatusCode.NotFound)
                return@post
            }
            if (!Permissions.isOrganizerOrAdmin(ctx.member, ctx.settings, event)) {
                call.respondText("出欠・受領記録の記録権限がありません", status = HttpStatusCode.Forbidden)
                return@post
            }
            val params = call.receiveParameters()
            val statuses = params.names()
                .filter { it.startsWith("status_") }
                .mapNotNull { key ->
                    val userId = key.removePrefix("status_").toLongOrNull() ?: return@mapNotNull null
                    val status = params[key] ?: return@mapNotNull null
                    userId to status
                }.toMap()
            val amounts = params.names()
                .filter { it.startsWith("amount_") }
                .mapNotNull { key ->
                    val userId = key.removePrefix("amount_").toLongOrNull() ?: return@mapNotNull null
                    // 空欄は受領記録の削除として 0 扱い
                    val value = params[key]?.trim() ?: return@mapNotNull null
                    val amount = if (value.isEmpty()) 0L else value.toLongOrNull() ?: return@mapNotNull null
                    userId to amount
                }.toMap()
            attendanceService.setAll(event, party.orderNo, statuses, ctx.session.userId)
            paymentService.setAll(event, party.orderNo, amounts, ctx.session.userId)
            call.respondRedirect("/g/${ctx.guild.idLong}/events/${event.id}#party-${party.id}")
        }

        post("/attendance/mark-all-present") {
            val ctx = requireGuildContext(gateway, settingsService) ?: return@post
            val partyId = call.parameters["partyId"]?.toLongOrNull()
            val party = partyId?.let { partyRepository.findById(it) }
            val event = party?.let { eventService.findById(it.eventId) }
            if (party == null || event == null || event.guildId != ctx.guild.idLong) {
                call.respondText("お店情報が見つかりません", status = HttpStatusCode.NotFound)
                return@post
            }
            if (!Permissions.isOrganizerOrAdmin(ctx.member, ctx.settings, event)) {
                call.respondText("出欠の記録権限がありません", status = HttpStatusCode.Forbidden)
                return@post
            }
            attendanceService.markAllPresent(event, party.orderNo, ctx.session.userId)
            call.respondRedirect("/g/${ctx.guild.idLong}/events/${event.id}")
        }
    }
}
