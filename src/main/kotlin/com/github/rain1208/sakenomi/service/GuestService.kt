package com.github.rain1208.sakenomi.service

import com.github.rain1208.sakenomi.discord.DiscordGateway
import com.github.rain1208.sakenomi.domain.Event
import com.github.rain1208.sakenomi.domain.Guest
import com.github.rain1208.sakenomi.domain.ServiceException
import com.github.rain1208.sakenomi.repository.GuestRepository
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member

/** ゲストの追加結果。restored = 削除済みゲストを同名で復活させた(出欠・受領記録も元に戻る) */
data class GuestAddResult(
    val guest: Guest,
    val restored: Boolean,
)

/**
 * ゲスト(Discord 未所持の参加者)管理。ロール付与は行わず、出欠・会計の記録対象としてのみ扱う。
 * 権限チェック(幹事/運営)は呼び出し側で行う。アーカイブ済みイベントでも操作可能
 */
class GuestService(
    private val guestRepository: GuestRepository,
    private val rosterService: RosterService,
    private val gateway: DiscordGateway,
    private val auditLogger: AuditLogger,
) {

    /** ゲスト追加。削除済みの同名ゲストがいれば復活させる */
    fun add(guild: Guild, executor: Member, event: Event, rawName: String): GuestAddResult {
        val name = normalizeName(rawName)
        val existing = guestRepository.findByName(event.id, name)
        val result = when {
            existing == null -> GuestAddResult(guestRepository.insert(event.id, name, executor.idLong), restored = false)
            existing.deletedAt != null -> {
                guestRepository.restore(existing.id)
                GuestAddResult(existing.copy(deletedAt = null), restored = true)
            }
            else -> throw ServiceException("ゲスト「$name」は既に登録されています")
        }
        val restoredNote = if (result.restored) "。以前の出欠・受領記録を復元しました" else ""
        gateway.sendChannelMessage(
            guild,
            event.channelId,
            "[ゲスト追加] **${result.guest.displayName}** を追加しました$restoredNote" +
                "(現在の参加者: ${rosterService.count(event.id).text})",
        )
        auditLogger.guestAdded(guild, event, result.guest, executor, result.restored)
        return result
    }

    /** ゲスト削除(論理削除)。出欠・受領記録は残し、集計からのみ外れる */
    fun remove(guild: Guild, executor: Member, event: Event, guest: Guest) {
        guestRepository.softDelete(guest.id)
        gateway.sendChannelMessage(
            guild,
            event.channelId,
            "[ゲスト削除] **${guest.displayName}** を削除しました(現在の参加者: ${rosterService.count(event.id).text})",
        )
        auditLogger.guestRemoved(guild, event, guest, executor)
    }

    fun rename(guild: Guild, executor: Member, event: Event, guest: Guest, rawName: String): Guest {
        val name = normalizeName(rawName)
        if (name == guest.name) return guest
        val conflict = guestRepository.findByName(event.id, name)
        if (conflict != null) {
            val suffix = if (conflict.deletedAt != null) "(削除済みのゲストと同じ名前は使えません)" else ""
            throw ServiceException("ゲスト「$name」は既に登録されています$suffix")
        }
        guestRepository.rename(guest.id, name)
        val renamed = guest.copy(name = name)
        auditLogger.guestRenamed(guild, event, guest.name, renamed, executor)
        return renamed
    }

    fun listActive(event: Event): List<Guest> = guestRepository.listActiveByEvent(event.id)

    /** 名前指定(コマンド用)。削除済みは対象外 */
    fun findActiveByName(event: Event, rawName: String): Guest =
        guestRepository.findByName(event.id, rawName.trim())
            ?.takeIf { it.deletedAt == null }
            ?: throw ServiceException("ゲスト「${rawName.trim()}」は見つかりません")

    /** ID 指定(Web 用)。他イベントのゲスト・削除済みは対象外 */
    fun findActiveById(event: Event, guestId: Long): Guest =
        guestRepository.findById(guestId)
            ?.takeIf { it.eventId == event.id && it.deletedAt == null }
            ?: throw ServiceException("ゲストが見つかりません(削除された可能性があります)")

    private fun normalizeName(rawName: String): String {
        val name = rawName.trim()
        if (name.isEmpty()) {
            throw ServiceException("ゲスト名を入力してください")
        }
        if (name.length > NAME_MAX_LENGTH) {
            throw ServiceException("ゲスト名は ${NAME_MAX_LENGTH} 文字以内で入力してください")
        }
        return name
    }

    companion object {
        const val NAME_MAX_LENGTH = 80
    }
}
