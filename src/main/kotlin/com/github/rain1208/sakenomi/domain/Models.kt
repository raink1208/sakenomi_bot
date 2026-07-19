package com.github.rain1208.sakenomi.domain

import java.time.LocalDate
import java.time.OffsetDateTime

/** ユーザー起因の業務エラー。メッセージをそのまま ephemeral 応答する */
class ServiceException(message: String) : RuntimeException(message)

enum class EventStatus {
    RECRUITING,
    ARCHIVED,
}

enum class SettingKey(val key: String, val label: String) {
    EVENT_CATEGORY("event-category", "イベントカテゴリ"),
    ARCHIVE_CATEGORY("archive-category", "アーカイブカテゴリ"),
    ADMIN_ROLE("admin-role", "運営ロール"),
    LOG_CHANNEL("log-channel", "ログチャンネル"),
    RECRUIT_CHANNEL("recruit-channel", "募集チャンネル"),
    ;

    companion object {
        fun from(key: String): SettingKey? = entries.firstOrNull { it.key == key }
    }
}

data class GuildSettings(
    val guildId: Long,
    val eventCategoryId: Long?,
    val archiveCategoryId: Long?,
    val adminRoleId: Long?,
    val logChannelId: Long?,
    val recruitChannelId: Long?,
) {
    val isComplete: Boolean
        get() = eventCategoryId != null &&
            archiveCategoryId != null &&
            adminRoleId != null &&
            logChannelId != null &&
            recruitChannelId != null
}

data class Event(
    val id: Long,
    val guildId: Long,
    val number: Int,
    val name: String,
    val area: String,
    val eventDate: LocalDate?,
    val status: EventStatus,
    val channelId: Long,
    val roleId: Long,
    val recruitMessageId: Long?,
    val organizerUserId: Long,
)

data class Party(
    val id: Long,
    val eventId: Long,
    val orderNo: Int,
    val shopName: String,
    val totalAmount: Long?,
    val note: String?,
)

data class Payment(
    val id: Long,
    val partyId: Long,
    val payerUserId: Long,
    val amount: Long,
    val recordedBy: Long,
    val receivedAt: OffsetDateTime,
)