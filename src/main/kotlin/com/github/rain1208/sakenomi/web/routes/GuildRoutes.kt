package com.github.rain1208.sakenomi.web.routes

import com.github.rain1208.sakenomi.discord.DiscordGateway
import com.github.rain1208.sakenomi.web.auth.requireSession
import io.ktor.server.freemarker.FreeMarkerContent
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/** ギルド選択(Web仕様書.md §5.2) */
fun Route.guildRoutes(gateway: DiscordGateway) {
    get("/") {
        val session = requireSession() ?: return@get
        val guilds = gateway.listMutualGuilds(session.userId)
        if (guilds.size == 1) {
            call.respondRedirect("/g/${guilds.first().idLong}/events")
            return@get
        }
        call.respond(
            FreeMarkerContent(
                "guild-select.ftl",
                mapOf(
                    "session" to session,
                    "guilds" to guilds.map { mapOf("id" to it.idLong, "name" to it.name) },
                ),
            ),
        )
    }
}
