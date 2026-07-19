package com.github.rain1208.sakenomi.web.auth

import io.ktor.server.sessions.SessionSerializer
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Web セッションの内容(Web仕様書.md §3.2)。
 * ロール・権限情報は保持せず、毎リクエスト DiscordGateway から解決する。
 */
data class UserSession(
    val userId: Long,
    val displayName: String,
    val avatarHash: String?,
    val issuedAt: Long,
) {
    companion object {
        const val MAX_AGE_MILLIS: Long = 7L * 24 * 60 * 60 * 1000
    }

    fun isExpired(): Boolean = System.currentTimeMillis() - issuedAt > MAX_AGE_MILLIS
}

/** kotlinx.serialization に依存しない単純な区切り文字シリアライザ */
object UserSessionSerializer : SessionSerializer<UserSession> {
    private const val CHARSET = "UTF-8"

    override fun serialize(session: UserSession): String {
        val avatar = session.avatarHash?.let { encode(it) } ?: ""
        return listOf(
            session.userId.toString(),
            encode(session.displayName),
            avatar,
            session.issuedAt.toString(),
        ).joinToString("|")
    }

    override fun deserialize(text: String): UserSession {
        val parts = text.split("|")
        require(parts.size >= 4) { "不正なセッション形式です" }
        return UserSession(
            userId = parts[0].toLong(),
            displayName = decode(parts[1]),
            avatarHash = parts[2].takeIf { it.isNotEmpty() }?.let { decode(it) },
            issuedAt = parts[3].toLong(),
        )
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
    private fun decode(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8.name())
}
