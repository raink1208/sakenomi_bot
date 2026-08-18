package com.github.rain1208.sakenomi.service

import com.github.rain1208.sakenomi.domain.AttendanceStatus
import com.github.rain1208.sakenomi.domain.Event
import com.github.rain1208.sakenomi.domain.Party
import com.github.rain1208.sakenomi.domain.Payment
import com.github.rain1208.sakenomi.domain.ServiceException
import com.github.rain1208.sakenomi.repository.AttendanceRepository
import com.github.rain1208.sakenomi.repository.PaymentRepository

data class PartyPayments(
    val party: Party,
    val payments: List<Payment>,
    val total: Long,
)

class PaymentService(
    private val partyService: PartyService,
    private val paymentRepository: PaymentRepository,
    private val attendanceRepository: AttendanceRepository,
) {

    /**
     * 受領記録の登録(§10.7)。割り勘計算は行わず受領実績のみ保持する。支払った人は出席したとみなす。
     * 同じお店・同じ支払者への再登録は金額を上書きする(1お店につき1人1レコード)。
     */
    fun add(event: Event, orderNo: Int, payerUserId: Long, amount: Long, recordedBy: Long): PartyPayments {
        if (amount <= 0) {
            throw ServiceException("金額は 1 円以上で指定してください")
        }
        val party = partyService.findByOrder(event, orderNo)
        paymentRepository.upsert(party.id, payerUserId, amount, recordedBy)
        attendanceRepository.upsert(party.id, payerUserId, AttendanceStatus.PRESENT, recordedBy)
        return PartyPayments(party, paymentRepository.listByParty(party.id), paymentRepository.totalByParty(party.id))
    }

    /**
     * 受領記録の一括更新(参加者一覧からの一斉更新用)。amounts は userId -> 金額(1円以上のみ反映、それ以外はスキップ)。
     * 金額を登録した人は出席したとみなす。
     */
    fun setAll(event: Event, orderNo: Int, amounts: Map<Long, Long>, recordedBy: Long): PartyPayments {
        val party = partyService.findByOrder(event, orderNo)
        amounts.filterValues { it > 0 }.forEach { (userId, amount) ->
            paymentRepository.upsert(party.id, userId, amount, recordedBy)
            attendanceRepository.upsert(party.id, userId, AttendanceStatus.PRESENT, recordedBy)
        }
        return PartyPayments(party, paymentRepository.listByParty(party.id), paymentRepository.totalByParty(party.id))
    }

    /** 受領一覧。orderNo 指定時はそのお店のみ */
    fun list(event: Event, orderNo: Int?): List<PartyPayments> {
        val parties = if (orderNo != null) {
            listOf(partyService.findByOrder(event, orderNo))
        } else {
            partyService.list(event)
        }
        return parties.map { party ->
            PartyPayments(party, paymentRepository.listByParty(party.id), paymentRepository.totalByParty(party.id))
        }
    }
}
