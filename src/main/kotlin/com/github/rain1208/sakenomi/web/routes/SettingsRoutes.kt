package com.github.rain1208.sakenomi.web.routes

import com.github.rain1208.sakenomi.discord.DiscordGateway
import com.github.rain1208.sakenomi.domain.ServiceException
import com.github.rain1208.sakenomi.domain.SettingKey
import com.github.rain1208.sakenomi.repository.EventRepository
import com.github.rain1208.sakenomi.service.SettingsService
import com.github.rain1208.sakenomi.web.auth.requireGuildContext
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

/** 設定画面(Web仕様書.md §5.5)。運営(admin-role)のみ */
fun Route.settingsRoutes(gateway: DiscordGateway, settingsService: SettingsService, eventRepository: EventRepository) {
    route("/g/{guildId}/settings") {
        get {
            val ctx = requireGuildContext(gateway, settingsService) ?: return@get
            if (!ctx.isAdmin) {
                call.respondText("設定変更は運営のみ実行できます", status = HttpStatusCode.Forbidden)
                return@get
            }
            val nextNumber = eventRepository.getNextNumber(ctx.guild.idLong)
            call.respond(
                FreeMarkerContent(
                    "settings.ftl",
                    settingsModel(gateway, ctx.session, ctx.guild.idLong, ctx.settings, nextNumber, null),
                ),
            )
        }

        post {
            val ctx = requireGuildContext(gateway, settingsService) ?: return@post
            if (!ctx.isAdmin) {
                call.respondText("設定変更は運営のみ実行できます", status = HttpStatusCode.Forbidden)
                return@post
            }
            val params = call.receiveParameters()
            for (key in SettingKey.entries) {
                params[key.key]?.toLongOrNull()?.let { value ->
                    settingsService.set(ctx.guild.idLong, key, value)
                }
            }
            params["next-number"]?.takeIf { it.isNotBlank() }?.let { raw ->
                val nextNumber = raw.toIntOrNull()?.takeIf { it >= 1 }
                    ?: throw ServiceException("次回イベント番号は 1 以上の整数で指定してください")
                val maxNumber = eventRepository.maxNumber(ctx.guild.idLong)
                if (maxNumber != null && nextNumber <= maxNumber) {
                    throw ServiceException("記録済みの最新イベントは第${maxNumber}回です。それより大きい番号を指定してください")
                }
                eventRepository.setNextNumber(ctx.guild.idLong, nextNumber)
            }
            val updated = settingsService.get(ctx.guild.idLong)
            val updatedNextNumber = eventRepository.getNextNumber(ctx.guild.idLong)
            call.respond(
                FreeMarkerContent(
                    "settings.ftl",
                    settingsModel(gateway, ctx.session, ctx.guild.idLong, updated, updatedNextNumber, "保存しました"),
                ),
            )
        }
    }
}

private fun settingsModel(
    gateway: DiscordGateway,
    session: com.github.rain1208.sakenomi.web.auth.UserSession,
    guildId: Long,
    settings: com.github.rain1208.sakenomi.domain.GuildSettings,
    nextNumber: Int,
    message: String?,
): Map<String, Any?> = mapOf(
    "session" to session,
    "guildId" to guildId,
    "isAdmin" to true,
    "message" to message,
    "categories" to gateway.listCategories(guildId).map { mapOf("id" to it.idLong, "name" to it.name) },
    "roles" to gateway.listRoles(guildId).map { mapOf("id" to it.idLong, "name" to it.name) },
    "channels" to gateway.listTextChannels(guildId).map { mapOf("id" to it.idLong, "name" to it.name) },
    "nextNumber" to nextNumber,
    "current" to mapOf(
        SettingKey.EVENT_CATEGORY.key to settings.eventCategoryId,
        SettingKey.ARCHIVE_CATEGORY.key to settings.archiveCategoryId,
        SettingKey.ADMIN_ROLE.key to settings.adminRoleId,
        SettingKey.LOG_CHANNEL.key to settings.logChannelId,
        SettingKey.RECRUIT_CHANNEL.key to settings.recruitChannelId,
    ),
)
