package com.github.rain1208.sakenomi.web

import com.github.rain1208.sakenomi.config.AppConfig
import com.github.rain1208.sakenomi.discord.DiscordGateway
import com.github.rain1208.sakenomi.domain.ServiceException
import com.github.rain1208.sakenomi.repository.EventRepository
import com.github.rain1208.sakenomi.repository.PartyRepository
import com.github.rain1208.sakenomi.repository.PaymentRepository
import com.github.rain1208.sakenomi.service.AttendanceService
import com.github.rain1208.sakenomi.service.EventService
import com.github.rain1208.sakenomi.service.GuestService
import com.github.rain1208.sakenomi.service.ParticipantService
import com.github.rain1208.sakenomi.service.PartyService
import com.github.rain1208.sakenomi.service.PaymentService
import com.github.rain1208.sakenomi.service.RosterService
import com.github.rain1208.sakenomi.service.SettingsService
import com.github.rain1208.sakenomi.web.auth.DiscordOAuthClient
import com.github.rain1208.sakenomi.web.auth.UserSession
import com.github.rain1208.sakenomi.web.auth.UserSessionSerializer
import com.github.rain1208.sakenomi.web.routes.authRoutes
import com.github.rain1208.sakenomi.web.routes.eventRoutes
import com.github.rain1208.sakenomi.web.routes.guildRoutes
import com.github.rain1208.sakenomi.web.routes.myPageRoutes
import com.github.rain1208.sakenomi.web.routes.partyPaymentRoutes
import com.github.rain1208.sakenomi.web.routes.settingsRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.freemarker.FreeMarker
import io.ktor.server.freemarker.FreeMarkerContent
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import io.ktor.server.sessions.SessionTransportTransformerEncrypt
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.util.hex
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.github.rain1208.sakenomi.web.WebServer")

/** Bot と同一 JVM で Ktor HTTP サーバーを起動する(Web仕様書.md §1) */
fun startWebServer(
    config: AppConfig,
    gateway: DiscordGateway,
    settingsService: SettingsService,
    eventService: EventService,
    participantService: ParticipantService,
    partyService: PartyService,
    paymentService: PaymentService,
    attendanceService: AttendanceService,
    guestService: GuestService,
    rosterService: RosterService,
    partyRepository: PartyRepository,
    paymentRepository: PaymentRepository,
    eventRepository: EventRepository,
) {
    val oauthClient = DiscordOAuthClient(config)

    embeddedServer(Netty, port = config.webPort) {
        install(XForwardedHeaders)

        install(FreeMarker) {
            templateLoader = freemarker.cache.ClassTemplateLoader(this::class.java.classLoader, "templates")
            // XSS対策: ${} 出力を既定でHTMLエスケープする(イベント名/店名/Discord表示名等はユーザーが自由に設定できるため)
            setOutputFormat(freemarker.core.HTMLOutputFormat.INSTANCE)
            autoEscapingPolicy = freemarker.template.Configuration.ENABLE_IF_SUPPORTED_AUTO_ESCAPING_POLICY
        }

        install(Sessions) {
            cookie<UserSession>("SAKENOMI_SESSION") {
                serializer = UserSessionSerializer
                cookie.path = "/"
                cookie.httpOnly = true
                cookie.extensions["SameSite"] = "Lax"
                cookie.maxAgeInSeconds = UserSession.MAX_AGE_MILLIS / 1000
                transform(
                    SessionTransportTransformerEncrypt(
                        hex(config.sessionEncryptKey),
                        hex(config.sessionSignKey),
                    ),
                )
            }
        }

        // CSRF 対策: 全 POST リクエストで Origin/Referer を検証する(Web仕様書.md §8)
        intercept(ApplicationCallPipeline.Plugins) {
            if (call.request.httpMethod == io.ktor.http.HttpMethod.Post) {
                val origin = call.request.headers["Origin"] ?: call.request.headers["Referer"]
                if (origin != null && !origin.startsWith(config.publicBaseUrl)) {
                    call.respondText("不正なリクエストです(Origin 不一致)", status = HttpStatusCode.Forbidden)
                    finish()
                }
            }
        }

        install(StatusPages) {
            exception<ServiceException> { call, cause ->
                call.respond(
                    HttpStatusCode.BadRequest,
                    FreeMarkerContent("error.ftl", mapOf("message" to cause.message)),
                )
            }
            exception<Throwable> { call, cause ->
                logger.error("Web コンソールで予期しないエラーが発生しました: {}", call.request.uri, cause)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    FreeMarkerContent("error.ftl", mapOf("message" to "予期しないエラーが発生しました")),
                )
            }
            status(HttpStatusCode.NotFound) { call, status ->
                call.respond(status, FreeMarkerContent("error.ftl", mapOf("message" to "ページが見つかりません")))
            }
            status(HttpStatusCode.Forbidden) { call, status ->
                call.respond(status, FreeMarkerContent("error.ftl", mapOf("message" to "権限がありません")))
            }
        }

        routing {
            staticResources("/static", "static")
            authRoutes(oauthClient)
            guildRoutes(gateway)
            eventRoutes(
                gateway, settingsService, eventService, participantService, partyService, paymentService, attendanceService,
                guestService, rosterService,
            )
            partyPaymentRoutes(
                gateway, settingsService, eventService, partyService, paymentService, attendanceService, partyRepository,
                rosterService, guestService,
            )
            myPageRoutes(gateway, settingsService, eventService, participantService, partyService, paymentRepository)
            settingsRoutes(gateway, settingsService, eventRepository)
        }
    }.start(wait = false)

    logger.info("Web コンソールを起動しました (port={})", config.webPort)
}
