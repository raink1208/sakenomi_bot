package com.github.rain1208.sakenomi.web.routes

import com.github.rain1208.sakenomi.web.auth.DiscordOAuthClient
import com.github.rain1208.sakenomi.web.auth.UserSession
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.server.freemarker.FreeMarkerContent
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.set
import io.ktor.server.sessions.sessions
import java.util.UUID

/** 認証ルーティング(Web仕様書.md §5.1) */
fun Route.authRoutes(oauthClient: DiscordOAuthClient) {
    get("/login") {
        call.respond(FreeMarkerContent("login.ftl", emptyMap<String, Any>()))
    }

    get("/oauth/login") {
        val state = UUID.randomUUID().toString()
        call.response.cookies.append(
            Cookie(
                name = "oauth_state",
                value = state,
                httpOnly = true,
                path = "/",
                maxAge = 600,
                extensions = mapOf("SameSite" to "Lax"),
            ),
        )
        call.respondRedirect("${oauthClient.authorizeUrl}&state=$state")
    }

    get("/oauth/callback") {
        val code = call.request.queryParameters["code"]
        val state = call.request.queryParameters["state"]
        val cookieState = call.request.cookies["oauth_state"]
        call.response.cookies.append(Cookie(name = "oauth_state", value = "", path = "/", maxAge = 0))

        if (code == null || state == null || state != cookieState) {
            call.respondText("認証に失敗しました(state が一致しません)。もう一度ログインしてください。", status = HttpStatusCode.BadRequest)
            return@get
        }

        val token = oauthClient.exchangeCodeForToken(code)
        val user = oauthClient.fetchUser(token)
        call.sessions.set(
            UserSession(
                userId = user.id,
                displayName = user.displayName,
                avatarHash = user.avatar,
                issuedAt = System.currentTimeMillis(),
            ),
        )
        call.respondRedirect("/")
    }

    post("/logout") {
        call.sessions.clear<UserSession>()
        call.respondRedirect("/login")
    }
}
