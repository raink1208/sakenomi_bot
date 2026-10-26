package com.github.rain1208.sakenomi.service

import com.github.rain1208.sakenomi.discord.DiscordGateway
import com.github.rain1208.sakenomi.domain.ParticipantIds
import com.github.rain1208.sakenomi.repository.GuestRepository
import com.github.rain1208.sakenomi.repository.ParticipantRepository

data class RosterCount(
    val memberCount: Long,
    val guestCount: Long,
) {
    val total: Long
        get() = memberCount + guestCount

    /** 「7人(うちゲスト2人)」。ゲストがいなければ「5人」 */
    val text: String
        get() = if (guestCount == 0L) "${total}人" else "${total}人(うちゲスト${guestCount}人)"
}

/**
 * イベントの現在の参加者(Discord メンバー + 削除されていないゲスト)を扱う。
 * 出欠・受領記録の対象者や集計はこの名簿を基準にする(退出したメンバー・削除したゲストの記録は残るが集計しない)
 */
class RosterService(
    private val participantRepository: ParticipantRepository,
    private val guestRepository: GuestRepository,
    private val gateway: DiscordGateway,
) {

    /** メンバー(参加順) → ゲスト(登録順)の参加者 ID 一覧。ゲストは負の疑似 ID */
    fun participantIds(eventId: Long): List<Long> =
        participantRepository.listUserIdsByEvent(eventId) +
            guestRepository.listActiveByEvent(eventId).map { it.participantId }

    fun count(eventId: Long): RosterCount = RosterCount(
        memberCount = participantRepository.countByEvent(eventId),
        guestCount = guestRepository.listActiveByEvent(eventId).size.toLong(),
    )

    /** 削除されていないゲストの疑似 ID → 表示名(「山田 (ゲスト)」) */
    fun guestLabels(eventId: Long): Map<Long, String> =
        guestRepository.listActiveByEvent(eventId).associate { it.participantId to it.displayName }

    /** Web 表示用の名前解決。メンバーは Discord の表示名、ゲストは「山田 (ゲスト)」 */
    fun resolveDisplayNames(guildId: Long, eventId: Long, participantIds: Collection<Long>): Map<Long, String> {
        val (guestIds, memberIds) = participantIds.distinct().partition { ParticipantIds.isGuest(it) }
        val guestNames = if (guestIds.isEmpty()) {
            emptyMap()
        } else {
            guestIds.associateWith { id ->
                guestRepository.findById(ParticipantIds.guestIdOf(id))
                    ?.takeIf { it.eventId == eventId }
                    ?.displayName
                    ?: "不明なゲスト"
            }
        }
        return gateway.resolveDisplayNames(guildId, memberIds) + guestNames
    }
}
