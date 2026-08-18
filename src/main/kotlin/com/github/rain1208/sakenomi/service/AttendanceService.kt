package com.github.rain1208.sakenomi.service

import com.github.rain1208.sakenomi.domain.AttendanceStatus
import com.github.rain1208.sakenomi.domain.Event
import com.github.rain1208.sakenomi.domain.Party
import com.github.rain1208.sakenomi.domain.ServiceException
import com.github.rain1208.sakenomi.repository.AttendanceRepository
import com.github.rain1208.sakenomi.repository.ParticipantRepository

/** null は未確認を表す */
data class AttendanceEntry(
    val userId: Long,
    val status: AttendanceStatus?,
)

data class PartyAttendances(
    val party: Party,
    val entries: List<AttendanceEntry>,
    val presentCount: Int,
    val absentCount: Int,
    val unconfirmedCount: Int,
)

class AttendanceService(
    private val partyService: PartyService,
    private val attendanceRepository: AttendanceRepository,
    private val participantRepository: ParticipantRepository,
) {

    /** 飲み会単位の出欠記録・修正。対象はイベント参加者のみ */
    fun set(event: Event, orderNo: Int, targetUserId: Long, status: AttendanceStatus, recordedBy: Long): PartyAttendances {
        val party = partyService.findByOrder(event, orderNo)
        if (!participantRepository.isParticipant(event.id, targetUserId)) {
            throw ServiceException("このユーザーはイベントに参加していません")
        }
        attendanceRepository.upsert(party.id, targetUserId, status, recordedBy)
        return buildSummary(event, party)
    }

    /**
     * 飲み会単位の出欠を一括更新する。statuses は userId -> "PRESENT"/"ABSENT"/"CLEAR"。
     * イベント参加者以外のキーは無視する。
     */
    fun setAll(event: Event, orderNo: Int, statuses: Map<Long, String>, recordedBy: Long): PartyAttendances {
        val party = partyService.findByOrder(event, orderNo)
        val participantIds = participantRepository.listUserIdsByEvent(event.id).toSet()
        statuses.filterKeys { it in participantIds }.forEach { (userId, statusParam) ->
            if (statusParam == "CLEAR") {
                attendanceRepository.delete(party.id, userId)
            } else {
                val status = AttendanceStatus.entries.find { it.name == statusParam } ?: return@forEach
                attendanceRepository.upsert(party.id, userId, status, recordedBy)
            }
        }
        return buildSummary(event, party)
    }

    /** 未確認の参加者だけ一括で出席にする(既に記録済みの人は上書きしない) */
    fun markAllPresent(event: Event, orderNo: Int, recordedBy: Long): PartyAttendances {
        val party = partyService.findByOrder(event, orderNo)
        val participantIds = participantRepository.listUserIdsByEvent(event.id)
        val recordedIds = attendanceRepository.listByParty(party.id).map { it.userId }.toSet()
        participantIds.filter { it !in recordedIds }
            .forEach { attendanceRepository.upsert(party.id, it, AttendanceStatus.PRESENT, recordedBy) }
        return buildSummary(event, party)
    }

    /** 未確認に戻す */
    fun clear(event: Event, orderNo: Int, targetUserId: Long): PartyAttendances {
        val party = partyService.findByOrder(event, orderNo)
        attendanceRepository.delete(party.id, targetUserId)
        return buildSummary(event, party)
    }

    /** 出欠一覧。orderNo 指定時はそのお店のみ */
    fun list(event: Event, orderNo: Int?): List<PartyAttendances> {
        val parties = if (orderNo != null) {
            listOf(partyService.findByOrder(event, orderNo))
        } else {
            partyService.list(event)
        }
        return parties.map { buildSummary(event, it) }
    }

    private fun buildSummary(event: Event, party: Party): PartyAttendances {
        val participantIds = participantRepository.listUserIdsByEvent(event.id)
        val recorded = attendanceRepository.listByParty(party.id).associateBy { it.userId }
        val entries = participantIds.map { AttendanceEntry(it, recorded[it]?.status) }
        return PartyAttendances(
            party = party,
            entries = entries,
            presentCount = entries.count { it.status == AttendanceStatus.PRESENT },
            absentCount = entries.count { it.status == AttendanceStatus.ABSENT },
            unconfirmedCount = entries.count { it.status == null },
        )
    }
}
