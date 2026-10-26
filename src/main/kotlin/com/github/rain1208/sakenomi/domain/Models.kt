package com.github.rain1208.sakenomi.domain

import java.time.LocalDate
import java.time.OffsetDateTime

/** ユーザー起因の業務エラー。メッセージをそのまま ephemeral 応答する */
class ServiceException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

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

/** イベント単位のゲスト(Discord 未所持の参加者)。幹事/運営が代理で登録する */
data class Guest(
    val id: Long,
    val eventId: Long,
    val name: String,
    val deletedAt: OffsetDateTime?,
) {
    /** attendances.user_id / payments.payer_user_id に格納する疑似 ID */
    val participantId: Long
        get() = ParticipantIds.ofGuest(id)

    val displayName: String
        get() = "$name (ゲスト)"
}

/**
 * 出欠・受領記録の参加者 ID。正の値は Discord ユーザー ID、負の値はゲストの疑似 ID(-event_guests.id)。
 * Discord のスノーフレークは常に正のため衝突しない。判定はここに集約し、生の比較を散らばらせない
 */
object ParticipantIds {
    fun isGuest(participantId: Long): Boolean = participantId < 0

    fun ofGuest(guestId: Long): Long = -guestId

    fun guestIdOf(participantId: Long): Long = -participantId
}

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
}

data class Attendance(
    val id: Long,
    val partyId: Long,
    val userId: Long,
    val status: AttendanceStatus,
    val recordedBy: Long,
    val recordedAt: OffsetDateTime,
)