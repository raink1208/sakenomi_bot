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
    private val rosterService: RosterService,
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
        val participantIds = rosterService.participantIds(event.id).toSet()
        if (payerUserId !in participantIds) {
            throw ServiceException("このユーザーはイベントに参加していません")
        }
        paymentRepository.upsert(party.id, payerUserId, amount, recordedBy)
        attendanceRepository.upsert(party.id, payerUserId, AttendanceStatus.PRESENT, recordedBy)
        return summarize(party, participantIds)
    }

    /**
     * 受領記録の一括更新(参加者一覧からの一斉更新用)。amounts は userId -> 金額(0 以下は受領記録の削除)。
     * 入力欄には現在の金額が入った状態で送信されるため、現在値から変わった人だけを反映する。
     * 金額を新たに登録・変更した人は出席したとみなす。イベント参加者(ゲスト含む)以外のキーは無視する。
     */
    fun setAll(event: Event, orderNo: Int, amounts: Map<Long, Long>, recordedBy: Long): PartyPayments {
        val party = partyService.findByOrder(event, orderNo)
        val participantIds = rosterService.participantIds(event.id).toSet()
        val current = paymentRepository.listByParty(party.id).associate { it.payerUserId to it.amount }
        amounts.filter { (userId, _) -> userId in participantIds }.forEach { (userId, amount) ->
            when {
                amount <= 0 -> if (userId in current) paymentRepository.delete(party.id, userId)
                current[userId] != amount -> {
                    paymentRepository.upsert(party.id, userId, amount, recordedBy)
                    attendanceRepository.upsert(party.id, userId, AttendanceStatus.PRESENT, recordedBy)
                }
            }
        }
        return summarize(party, participantIds)
    }

    /** 受領一覧。orderNo 指定時はそのお店のみ */
    fun list(event: Event, orderNo: Int?): List<PartyPayments> {
        val parties = if (orderNo != null) {
            listOf(partyService.findByOrder(event, orderNo))
        } else {
            partyService.list(event)
        }
        val participantIds = rosterService.participantIds(event.id).toSet()
        return parties.map { summarize(it, participantIds) }
    }

    /**
     * 現在の参加者(退出していないメンバー・削除されていないゲスト)の受領記録のみを一覧・合計に含める。
     * 退出・削除した人の記録は DB に残り、再参加(ゲストは同名で再追加)すると集計に戻る
     */
    private fun summarize(party: Party, participantIds: Set<Long>): PartyPayments {
        val payments = paymentRepository.listByParty(party.id).filter { it.payerUserId in participantIds }
        return PartyPayments(party, payments, payments.sumOf { it.amount })
    }
}
