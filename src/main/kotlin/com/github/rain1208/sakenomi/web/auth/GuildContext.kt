package com.github.rain1208.sakenomi.web.auth

import com.github.rain1208.sakenomi.discord.DiscordGateway
import com.github.rain1208.sakenomi.domain.GuildSettings
import com.github.rain1208.sakenomi.service.SettingsService
import com.github.rain1208.sakenomi.util.Permissions
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member

/** ギルドにスコープされたリクエストの実行コンテキスト(Web仕様書.md §3.3) */
data class GuildContext(
    val guild: Guild,
    val member: Member,
    val session: UserSession,
    val settings: GuildSettings,
    val isAdmin: Boolean,
)

suspend fun ApplicationCall.currentSession(): UserSession? {
    val session = sessions.get<UserSession>() ?: return null
    if (session.isExpired()) {
        sessions.clear<UserSession>()
        return null
    }
    return session
}

/** 未ログイン時は /login へリダイレクトして null を返す */
suspend fun RoutingContext.requireSession(): UserSession? {
    val session = call.currentSession()
    if (session == null) {
        call.respondRedirect("/login")
    }
    return session
}

/**
 * ギルドメンバーシップ確認込みのコンテキスト解決(Web仕様書.md §3.3)。
 * 未ログインは /login へ、非メンバーは 403 を返して null を返す。
 */
suspend fun RoutingContext.requireGuildContext(
    gateway: DiscordGateway,
    settingsService: SettingsService,
): GuildContext? {
    val session = requireSession() ?: return null
    val guildId = call.parameters["guildId"]?.toLongOrNull()
    if (guildId == null) {
        call.respondText("不正なリクエストです", status = HttpStatusCode.BadRequest)
        return null
    }
    val guild = gateway.findGuild(guildId)
    if (guild == null) {
        call.respondText("サーバーが見つかりません", status = HttpStatusCode.NotFound)
        return null
    }
    val member = gateway.findMember(guildId, session.userId)
    if (member == null) {
        call.respondText("このサーバーのメンバーではありません", status = HttpStatusCode.Forbidden)
        return null
    }
    val settings = settingsService.get(guildId)
    return GuildContext(guild, member, session, settings, Permissions.isAdmin(member, settings))
}
