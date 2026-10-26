package com.github.rain1208.sakenomi.service

import com.github.rain1208.sakenomi.discord.DiscordGateway
import com.github.rain1208.sakenomi.domain.Event
import com.github.rain1208.sakenomi.domain.EventStatus
import com.github.rain1208.sakenomi.domain.ServiceException
import com.github.rain1208.sakenomi.repository.EventRepository
import com.github.rain1208.sakenomi.repository.ParticipantRepository
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member

class ParticipantService(
    private val eventRepository: EventRepository,
    private val participantRepository: ParticipantRepository,
    private val rosterService: RosterService,
    private val gateway: DiscordGateway,
    private val auditLogger: AuditLogger,
) {

    /** イベント参加(§10.3) */
    fun join(guild: Guild, member: Member, eventId: Long): Event {
        val event = findGuildEvent(guild, eventId)
        if (event.status != EventStatus.RECRUITING) {
            throw ServiceException("このイベントは募集を終了しています")
        }
        if (participantRepository.isParticipant(event.id, member.idLong)) {
            throw ServiceException("既に参加しています")
        }
        participantRepository.add(event.id, member.idLong)
        try {
            gateway.grantRole(guild, member, event.roleId)
        } catch (e: Exception) {
            participantRepository.remove(event.id, member.idLong)
            throw ServiceException("ロール付与に失敗しました。Bot の権限を確認してください")
        }
        val remaining = rosterService.count(event.id).text
        gateway.sendChannelMessage(
            guild,
            event.channelId,
            "[参加通知] **${member.effectiveName}** が参加しました(現在の参加者: $remaining)",
        )
        return event
    }

    /** 参加キャンセル(§10.4) */
    fun leave(guild: Guild, member: Member, eventId: Long): Event {
        val event = findGuildEvent(guild, eventId)
        if (event.status == EventStatus.ARCHIVED) {
            throw ServiceException("アーカイブ済みイベントの参加はキャンセルできません")
        }
        if (event.organizerUserId == member.idLong) {
            throw ServiceException("幹事は参加をキャンセルできません")
        }
        if (!participantRepository.remove(event.id, member.idLong)) {
            throw ServiceException("このイベントに参加していません")
        }
        gateway.removeRole(guild, member, event.roleId)
        val remaining = rosterService.count(event.id).text
        gateway.sendChannelMessage(
            guild,
            event.channelId,
            "[退出通知] **${member.effectiveName}** が退出しました(現在の参加者: $remaining)",
        )
        return event
    }

    /** 運営による参加者強制追加(§10.3 拡張) */
    fun adminAdd(guild: Guild, executor: Member, targetUserId: Long, eventId: Long): Event {
        val event = findGuildEvent(guild, eventId)
        if (participantRepository.isParticipant(event.id, targetUserId)) {
            throw ServiceException("既に参加しています")
        }
        val targetMember = gateway.findMember(guild.idLong, targetUserId)
            ?: throw ServiceException("対象ユーザーがサーバーに見つかりません")
        participantRepository.add(event.id, targetUserId)
        try {
            gateway.grantRole(guild, targetMember, event.roleId)
        } catch (e: Exception) {
            participantRepository.remove(event.id, targetUserId)
            throw ServiceException("ロール付与に失敗しました。Bot の権限を確認してください")
        }
        val remaining = rosterService.count(event.id).text
        gateway.sendChannelMessage(
            guild,
            event.channelId,
            "[参加通知] **${targetMember.effectiveName}** が参加しました(現在の参加者: $remaining)",
        )
        auditLogger.participantAdded(guild, event, targetMember, executor)
        return event
    }

    /** 運営による参加者強制削除(§10.4 拡張) */
    fun adminRemove(guild: Guild, executor: Member, targetUserId: Long, eventId: Long): Event {
        val event = findGuildEvent(guild, eventId)
        if (event.organizerUserId == targetUserId) {
            throw ServiceException("幹事は参加者リストから削除できません")
        }
        if (!participantRepository.remove(event.id, targetUserId)) {
            throw ServiceException("このユーザーはイベントに参加していません")
        }
        val targetMember = gateway.findMember(guild.idLong, targetUserId)
        targetMember?.let { gateway.removeRole(guild, it, event.roleId) }
        val displayName = targetMember?.effectiveName ?: "不明なユーザー($targetUserId)"
        val remaining = rosterService.count(event.id).text
        gateway.sendChannelMessage(
            guild,
            event.channelId,
            "[退出通知] **$displayName** が退出しました(現在の参加者: $remaining)",
        )
        auditLogger.participantRemoved(guild, event, targetUserId, executor)
        return event
    }

    fun isParticipant(eventId: Long, userId: Long): Boolean =
        participantRepository.isParticipant(eventId, userId)

    /** ゲストを含む参加人数 */
    fun count(eventId: Long): RosterCount = rosterService.count(eventId)

    /** Web コンソールの参加者一覧・支払者選択用(Discord メンバーのみ。ゲストは GuestService) */
    fun listUserIds(eventId: Long): List<Long> = participantRepository.listUserIdsByEvent(eventId)

    /** マイページ用: ユーザーが参加しているイベント ID 一覧 */
    fun listEventIdsByUser(userId: Long): List<Long> = participantRepository.listEventIdsByUser(userId)

    private fun findGuildEvent(guild: Guild, eventId: Long): Event =
        eventRepository.findById(eventId)
            ?.takeIf { it.guildId == guild.idLong }
            ?: throw ServiceException("イベントが見つかりません(削除された可能性があります)")
}
