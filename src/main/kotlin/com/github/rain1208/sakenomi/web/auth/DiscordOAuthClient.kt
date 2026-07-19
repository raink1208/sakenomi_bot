package com.github.rain1208.sakenomi.web.auth

import com.github.rain1208.sakenomi.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class DiscordUser(
    val id: Long,
    val username: String,
    val globalName: String?,
    val avatar: String?,
) {
    val displayName: String get() = globalName ?: username
}

/** Discord OAuth2 認可コードフロー(identify スコープのみ、Web仕様書.md §3.1) */
class DiscordOAuthClient(private val config: AppConfig) {

    private val client = HttpClient(CIO)
    private val json = Json { ignoreUnknownKeys = true }

    val authorizeUrl: String
        get() = "https://discord.com/api/oauth2/authorize" +
            "?client_id=${config.discordClientId}" +
            "&redirect_uri=${redirectUri()}" +
            "&response_type=code" +
            "&scope=identify"

    private fun redirectUri(): String = "${config.publicBaseUrl}/oauth/callback"

    suspend fun exchangeCodeForToken(code: String): String {
        val response = client.submitForm(
            url = "https://discord.com/api/oauth2/token",
            formParameters = Parameters.build {
                append("client_id", config.discordClientId)
                append("client_secret", config.discordClientSecret)
                append("grant_type", "authorization_code")
                append("code", code)
                append("redirect_uri", redirectUri())
            },
        )
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        return body["access_token"]?.jsonPrimitive?.content
            ?: error("Discord トークン交換に失敗しました: ${response.bodyAsText()}")
    }

    suspend fun fetchUser(accessToken: String): DiscordUser {
        val response = client.get("https://discord.com/api/users/@me") {
            header("Authorization", "Bearer $accessToken")
        }
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        return DiscordUser(
            id = body["id"]!!.jsonPrimitive.content.toLong(),
            username = body["username"]!!.jsonPrimitive.content,
            globalName = body["global_name"]?.jsonPrimitive?.contentOrNull(),
            avatar = body["avatar"]?.jsonPrimitive?.contentOrNull(),
        )
    }

    private fun JsonPrimitive.contentOrNull(): String? =
        if (this.isString) this.content else null
}
