package com.github.rain1208.sakenomi.web.routes

import com.github.rain1208.sakenomi.discord.DiscordGateway
import com.github.rain1208.sakenomi.domain.Event
import com.github.rain1208.sakenomi.domain.EventStatus
import com.github.rain1208.sakenomi.domain.ServiceException
import com.github.rain1208.sakenomi.service.AttendanceService
import com.github.rain1208.sakenomi.service.EventService
import com.github.rain1208.sakenomi.service.GuestService
import com.github.rain1208.sakenomi.service.ParticipantService
import com.github.rain1208.sakenomi.service.PartyService
import com.github.rain1208.sakenomi.service.PaymentService
import com.github.rain1208.sakenomi.service.RosterService
import com.github.rain1208.sakenomi.service.SettingsService
import com.github.rain1208.sakenomi.util.Permissions
import com.github.rain1208.sakenomi.web.auth.GuildContext
import com.github.rain1208.sakenomi.web.auth.requireGuildContext
import com.github.rain1208.sakenomi.web.viewmodel.ViewModels
import io.ktor.http.HttpStatusCode
import io.ktor.server.freemarker.FreeMarkerContent
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.LocalDate

/** イベント関連ルーティング(Web仕様書.md §5.3) */
fun Route.eventRoutes(
    gateway: DiscordGateway,
    settingsService: SettingsService,
    eventService: EventService,
    participantService: ParticipantService,
    partyService: PartyService,
    paymentService: PaymentService,
    attendanceService: AttendanceService,
    guestService: GuestService,
    rosterService: RosterService,
) {
    route("/g/{guildId}/events") {
        get {
            val ctx = requireGuildContext(gateway, settingsService) ?: return@get
            val statusParam = call.request.queryParameters["status"]?.takeIf { it == "archived" } ?: "recruiting"
            val status = if (statusParam == "archived") EventStatus.ARCHIVED else EventStatus.RECRUITING

            val events = eventService.listByGuild(ctx.guild.idLong, status)
                .filter { canView(it, ctx, participantService) }
                .map { event ->
                    ViewModels.event(
                        event,
                        participantCount = participantService.count(event.id).text,
                        organizerName = gateway.resolveDisplayNames(ctx.guild.idLong, listOf(event.organizerUserId))[event.organizerUserId],
                    )
                }

            call.respond(
                FreeMarkerContent(
                    "event-list.ftl",
                    mapOf(
                        "session" to ctx.session,
                        "guildId" to ctx.guild.idLong,
                        "isAdmin" to ctx.isAdmin,
                        "status" to statusParam,
                        "events" to events,
                    ),
                ),
            )
        }

        get("/new") {
            val ctx = requireGuildContext(gateway, settingsService) ?: return@get
            call.respond(
                FreeMarkerContent(
                    "event-form.ftl",
                    mapOf(
                        "session" to ctx.session,
                        "guildId" to ctx.guild.idLong,
                        "mode" to "create",
                        "form" to mapOf("name" to "", "area" to "", "date" to ""),
                        "error" to null,
                    ),
                ),
            )
        }

        post {
            val ctx = requireGuildContext(gateway, settingsService) ?: return@post
            val params = call.receiveParameters()
            val name = params["name"]?.trim().orEmpty()
            val area = params["area"]?.trim().orEmpty()
            val dateText = params["date"]?.trim().orEmpty()

            try {
                if (name.isBlank() || area.isBlank()) {
                    throw ServiceException("イベント名とエリアは必須です")
                }
                val date = parseDate(dateText)
                val created = eventService.create(ctx.guild, ctx.member, name, area, date)
                call.respondRedirect("/g/${ctx.guild.idLong}/events/${created.id}")
            } catch (e: ServiceException) {
                call.respond(
                    FreeMarkerContent(
                        "event-form.ftl",
                        mapOf(
                            "session" to ctx.session,
                            "guildId" to ctx.guild.idLong,
                            "mode" to "create",
                            "form" to mapOf("name" to name, "area" to area, "date" to dateText),
                            "error" to e.message,
                        ),
                    ),
                )
            }
        }

        route("/{eventId}") {
            get {
                val ctx = requireGuildContext(gateway, settingsService) ?: return@get
                val event = resolveEvent(eventService, ctx.guild.idLong) ?: return@get
                if (!canView(event, ctx, participantService)) {
                    call.respondText("このイベントを閲覧する権限がありません", status = HttpStatusCode.Forbidden)
                    return@get
                }

                val payments = paymentService.list(event, null)
                val attendances = attendanceService.list(event, null).associateBy { it.party.id }
                val canManage = Permissions.isOrganizerOrAdmin(ctx.member, ctx.settings, event)
                val isParticipant = participantService.isParticipant(event.id, ctx.session.userId)
                val participantIds = participantService.listUserIds(event.id)
                val guests = guestService.listActive(event)
                val userIds = participantIds.toMutableSet()
                userIds.add(event.organizerUserId)
                guests.forEach { userIds.add(it.participantId) }
                val names = rosterService.resolveDisplayNames(ctx.guild.idLong, event.id, userIds)

                call.respond(
                    FreeMarkerContent(
                        "event-detail.ftl",
                        mapOf(
                            "session" to ctx.session,
                            "guildId" to ctx.guild.idLong,
                            "event" to ViewModels.event(event, participantService.count(event.id).text, names[event.organizerUserId]),
                            "canManage" to canManage,
                            "isAdmin" to ctx.isAdmin,
                            "isOrganizer" to (event.organizerUserId == ctx.session.userId),
                            "isParticipant" to isParticipant,
                            "participants" to participantIds.map {
                                mapOf(
                                    "id" to it,
                                    "name" to (names[it] ?: it.toString()),
                                    "isOrganizer" to (it == event.organizerUserId),
                                )
                            },
                            "guests" to guests.map { mapOf("id" to it.id, "name" to it.name, "displayName" to it.displayName) },
                            "parties" to payments.map { pp ->
                                val paymentsByUser = pp.payments.associateBy { it.payerUserId }
                                ViewModels.party(pp.party, pp.total) + mapOf(
                                    "attendance" to attendances[pp.party.id]?.entries.orEmpty().map { entry ->
                                        ViewModels.attendanceEntry(
                                            entry,
                                            names[entry.userId] ?: entry.userId.toString(),
                                            paymentsByUser[entry.userId]?.amount,
                                        )
                                    },
                                )
                            },
                        ),
                    ),
                )
            }

            // ギルドメンバー一覧の取得(Discord API)は数秒〜数十秒かかることがあるため、
            // イベント詳細ページの表示をブロックしないよう別リクエストとして非同期に取得する
            get("/addable-members") {
                val ctx = requireGuildContext(gateway, settingsService) ?: return@get
                val event = resolveEvent(eventService, ctx.guild.idLong) ?: return@get
                if (!ctx.isAdmin) {
                    call.respondText("参加者の追加は運営のみ実行できます", status = HttpStatusCode.Forbidden)
                    return@get
                }
                val participantIds = participantService.listUserIds(event.id)
                val addableMembers = gateway.listMembers(ctx.guild.idLong)
                    .filter { !it.user.isBot && it.idLong !in participantIds }
                    .sortedBy { it.effectiveName }
                    .map { mapOf("id" to it.idLong, "name" to it.effectiveName) }
                call.respond(
                    FreeMarkerContent(
                        "event-addable-members.ftl",
                        mapOf("addableMembers" to addableMembers),
                    ),
                )
            }

            // ギルドメンバー一覧の取得(Discord API)は数秒〜数十秒かかることがあるため、
            // イベント詳細ページの表示をブロックしないよう別リクエストとして非同期に取得する
            get("/organizer-candidates") {
                val ctx = requireGuildContext(gateway, settingsService) ?: return@get
                val event = resolveEvent(eventService, ctx.guild.idLong) ?: return@get
                if (!Permissions.isOrganizerOrAdmin(ctx.member, ctx.settings, event)) {
                    call.respondText("幹事の変更は幹事または運営のみ実行できます", status = HttpStatusCode.Forbidden)
                    return@get
                }
                val participantUserIds = participantService.listUserIds(event.id).toSet()
                val candidates = gateway.listMembers(ctx.guild.idLong)
                    .filter { !it.user.isBot && it.idLong != event.organizerUserId && it.idLong in participantUserIds }
                    .sortedBy { it.effectiveName }
                    .map { mapOf("id" to it.idLong, "name" to it.effectiveName) }
                call.respond(
                    FreeMarkerContent(
                        "event-organizer-candidates.ftl",
                        mapOf("candidates" to candidates),
                    ),
                )
            }

            post("/transfer-organizer") {
                val ctx = requireGuildContext(gateway, settingsService) ?: return@post
                val event = resolveEvent(eventService, ctx.guild.idLong) ?: return@post
                if (!Permissions.isOrganizerOrAdmin(ctx.member, ctx.settings, event)) {
                    call.respondText("幹事の変更は幹事または運営のみ実行できます", status = HttpStatusCode.Forbidden)
                    return@post
                }
                val userId = call.receiveParameters()["userId"]?.toLongOrNull()
                    ?: throw ServiceException("新しい幹事を選択してください")
                eventService.transferOrganizer(ctx.guild, event, ctx.member, userId)
                call.respondRedirect("/g/${ctx.guild.idLong}/events/${event.id}")
            }

            get("/edit") {
                val ctx = requireGuildContext(gateway, settingsService) ?: return@get
                val event = resolveEvent(eventService, ctx.guild.idLong) ?: return@get
                if (!Permissions.isOrganizerOrAdmin(ctx.member, ctx.settings, event)) {
                    call.respondText("編集権限がありません", status = HttpStatusCode.Forbidden)
                    return@get
                }
                call.respond(
                    FreeMarkerContent(
                        "event-form.ftl",
                        mapOf(
                            "session" to ctx.session,
                            "guildId" to ctx.guild.idLong,
                            "mode" to "edit",
                            "eventId" to event.id,
                            "form" to mapOf(
                                "name" to event.name,
                                "area" to event.area,
                                "date" to (event.eventDate?.toString() ?: ""),
                            ),
                            "error" to null,
                        ),
                    ),
                )
            }

            post("/edit") {
                val ctx = requireGuildContext(gateway, settingsService) ?: return@post
                val event = resolveEvent(eventService, ctx.guild.idLong) ?: return@post
                if (!Permissions.isOrganizerOrAdmin(ctx.member, ctx.settings, event)) {
                    call.respondText("編集権限がありません", status = HttpStatusCode.Forbidden)
                    return@post
                }
                val params = call.receiveParameters()
                val name = params["name"]?.trim().orEmpty()
                val area = params["area"]?.trim().orEmpty()
                val dateText = params["date"]?.trim().orEmpty()
                try {
                    if (name.isBlank() || area.isBlank()) {
                        throw ServiceException("イベント名とエリアは必須です")
                    }
                    val date = parseDate(dateText)
                    eventService.edit(ctx.guild, event, name, area, date)
                    call.respondRedirect("/g/${ctx.guild.idLong}/events/${event.id}")
                } catch (e: ServiceException) {
                    call.respond(
                        FreeMarkerContent(
                            "event-form.ftl",
                            mapOf(
                                "session" to ctx.session,
                                "guildId" to ctx.guild.idLong,
                                "mode" to "edit",
                                "eventId" to event.id,
                                "form" to mapOf("name" to name, "area" to area, "date" to dateText),
                                "error" to e.message,
                            ),
                        ),
                    )
                }
            }

            post("/join") {
                val ctx = requireGuildContext(gateway, settingsService) ?: return@post
                val event = resolveEvent(eventService, ctx.guild.idLong) ?: return@post
                participantService.join(ctx.guild, ctx.member, event.id)
                call.respondRedirect("/g/${ctx.guild.idLong}/events/${event.id}")
            }

            post("/leave") {
                val ctx = requireGuildContext(gateway, settingsService) ?: return@post
                val event = resolveEvent(eventService, ctx.guild.idLong) ?: return@post
                participantService.leave(ctx.guild, ctx.member, event.id)
                call.respondRedirect("/g/${ctx.guild.idLong}/events/${event.id}")
            }

            post("/archive") {
                val ctx = requireGuildContext(gateway, settingsService) ?: return@post
                val event = resolveEvent(eventService, ctx.guild.idLong) ?: return@post
                if (!Permissions.isOrganizerOrAdmin(ctx.member, ctx.settings, event)) {
                    call.respondText("アーカイブ権限がありません", status = HttpStatusCode.Forbidden)
                    return@post
                }
                eventService.archive(ctx.guild, event)
                call.respondRedirect("/g/${ctx.guild.idLong}/events/${event.id}")
            }

            post("/reopen") {
                val ctx = requireGuildContext(gateway, settingsService) ?: return@post
                val event = resolveEvent(eventService, ctx.guild.idLong) ?: return@post
                if (!Permissions.isOrganizerOrAdmin(ctx.member, ctx.settings, event)) {
                    call.respondText("アーカイブ解除の権限がありません", status = HttpStatusCode.Forbidden)
                    return@post
                }
                eventService.reopen(ctx.guild, event)
                call.respondRedirect("/g/${ctx.guild.idLong}/events/${event.id}")
            }

            get("/delete") {
                val ctx = requireGuildContext(gateway, settingsService) ?: return@get
                val event = resolveEvent(eventService, ctx.guild.idLong) ?: return@get
                if (!ctx.isAdmin) {
                    call.respondText("削除権限がありません", status = HttpStatusCode.Forbidden)
                    return@get
                }
                call.respond(
                    FreeMarkerContent(
                        "event-delete-confirm.ftl",
                        mapOf(
                            "session" to ctx.session,
                            "guildId" to ctx.guild.idLong,
                            "event" to ViewModels.event(event),
                        ),
                    ),
                )
            }

            post("/delete") {
                val ctx = requireGuildContext(gateway, settingsService) ?: return@post
                val event = resolveEvent(eventService, ctx.guild.idLong) ?: return@post
                if (!ctx.isAdmin) {
                    call.respondText("削除権限がありません", status = HttpStatusCode.Forbidden)
                    return@post
                }
                eventService.delete(ctx.guild, event, ctx.member)
                call.respondRedirect("/g/${ctx.guild.idLong}/events")
            }

            post("/renumber") {
                val ctx = requireGuildContext(gateway, settingsService) ?: return@post
                val event = resolveEvent(eventService, ctx.guild.idLong) ?: return@post
                if (!ctx.isAdmin) {
                    call.respondText("採番修正の権限がありません", status = HttpStatusCode.Forbidden)
                    return@post
                }
                val newNumber = call.receiveParameters()["number"]?.toIntOrNull()
                    ?: throw ServiceException("番号は整数で指定してください")
                eventService.renumber(ctx.guild, event, newNumber, ctx.member)
                call.respondRedirect("/g/${ctx.guild.idLong}/events/${event.id}")
            }

            post("/add-user") {
                val ctx = requireGuildContext(gateway, settingsService) ?: return@post
                val event = resolveEvent(eventService, ctx.guild.idLong) ?: return@post
                if (!ctx.isAdmin) {
                    call.respondText("参加者の追加は運営のみ実行できます", status = HttpStatusCode.Forbidden)
                    return@post
                }
                val userId = call.receiveParameters()["userId"]?.toLongOrNull()
                    ?: throw ServiceException("追加するユーザーを選択してください")
                participantService.adminAdd(ctx.guild, ctx.member, userId, event.id)
                call.respondRedirect("/g/${ctx.guild.idLong}/events/${event.id}")
            }

            post("/remove-user") {
                val ctx = requireGuildContext(gateway, settingsService) ?: return@post
                val event = resolveEvent(eventService, ctx.guild.idLong) ?: return@post
                if (!ctx.isAdmin) {
                    call.respondText("参加者の削除は運営のみ実行できます", status = HttpStatusCode.Forbidden)
                    return@post
                }
                val userId = call.receiveParameters()["userId"]?.toLongOrNull()
                    ?: throw ServiceException("削除するユーザーを指定してください")
                participantService.adminRemove(ctx.guild, ctx.member, userId, event.id)
                call.respondRedirect("/g/${ctx.guild.idLong}/events/${event.id}")
            }

            post("/guests") {
                val ctx = requireGuildContext(gateway, settingsService) ?: return@post
                val event = resolveEvent(eventService, ctx.guild.idLong) ?: return@post
                if (!Permissions.isOrganizerOrAdmin(ctx.member, ctx.settings, event)) {
                    call.respondText("ゲストの追加は幹事または運営のみ実行できます", status = HttpStatusCode.Forbidden)
                    return@post
                }
                val name = call.receiveParameters()["name"].orEmpty()
                guestService.add(ctx.guild, ctx.member, event, name)
                call.respondRedirect("/g/${ctx.guild.idLong}/events/${event.id}")
            }

            post("/guests/{guestId}/rename") {
                val ctx = requireGuildContext(gateway, settingsService) ?: return@post
                val event = resolveEvent(eventService, ctx.guild.idLong) ?: return@post
                if (!Permissions.isOrganizerOrAdmin(ctx.member, ctx.settings, event)) {
                    call.respondText("ゲスト名の変更は幹事または運営のみ実行できます", status = HttpStatusCode.Forbidden)
                    return@post
                }
                val guestId = call.parameters["guestId"]?.toLongOrNull()
                    ?: throw ServiceException("ゲストが見つかりません")
                val guest = guestService.findActiveById(event, guestId)
                val name = call.receiveParameters()["name"].orEmpty()
                guestService.rename(ctx.guild, ctx.member, event, guest, name)
                call.respondRedirect("/g/${ctx.guild.idLong}/events/${event.id}")
            }

            post("/guests/{guestId}/delete") {
                val ctx = requireGuildContext(gateway, settingsService) ?: return@post
                val event = resolveEvent(eventService, ctx.guild.idLong) ?: return@post
                if (!Permissions.isOrganizerOrAdmin(ctx.member, ctx.settings, event)) {
                    call.respondText("ゲストの削除は幹事または運営のみ実行できます", status = HttpStatusCode.Forbidden)
                    return@post
                }
                val guestId = call.parameters["guestId"]?.toLongOrNull()
                    ?: throw ServiceException("ゲストが見つかりません")
                val guest = guestService.findActiveById(event, guestId)
                guestService.remove(ctx.guild, ctx.member, event, guest)
                call.respondRedirect("/g/${ctx.guild.idLong}/events/${event.id}")
            }

            post("/parties") {
                val ctx = requireGuildContext(gateway, settingsService) ?: return@post
                val event = resolveEvent(eventService, ctx.guild.idLong) ?: return@post
                if (!Permissions.isOrganizerOrAdmin(ctx.member, ctx.settings, event)) {
                    call.respondText("飲み会登録の権限がありません", status = HttpStatusCode.Forbidden)
                    return@post
                }
                val params = call.receiveParameters()
                val shopName = params["shopName"]?.trim().orEmpty()
                val orderNo = params["orderNo"]?.toIntOrNull()
                val totalAmount = params["totalAmount"]?.takeIf { it.isNotBlank() }?.toLongOrNull()
                val note = params["note"]?.trim()?.takeIf { it.isNotBlank() }
                partyService.add(event, shopName, orderNo, totalAmount, note)
                call.respondRedirect("/g/${ctx.guild.idLong}/events/${event.id}")
            }
        }
    }
}

private fun canView(event: Event, ctx: GuildContext, participantService: ParticipantService): Boolean {
    if (event.status == EventStatus.RECRUITING) return true
    return ctx.isAdmin ||
        event.organizerUserId == ctx.session.userId ||
        participantService.isParticipant(event.id, ctx.session.userId)
}

private suspend fun RoutingContext.resolveEvent(eventService: EventService, guildId: Long): Event? {
    val eventId = call.parameters["eventId"]?.toLongOrNull()
    val event = eventId?.let { eventService.findById(it) }
    if (event == null || event.guildId != guildId) {
        call.respondText("イベントが見つかりません", status = HttpStatusCode.NotFound)
        return null
    }
    return event
}

private fun parseDate(value: String): LocalDate? {
    if (value.isBlank()) return null
    return try {
        LocalDate.parse(value)
    } catch (e: Exception) {
        throw ServiceException("日付は YYYY-MM-DD 形式で指定してください")
    }
}
