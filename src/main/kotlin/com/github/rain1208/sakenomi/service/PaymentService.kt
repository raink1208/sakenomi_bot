package com.github.rain1208.sakenomi.service

import com.github.rain1208.sakenomi.domain.Event
import com.github.rain1208.sakenomi.domain.Party
import com.github.rain1208.sakenomi.domain.Payment
import com.github.rain1208.sakenomi.domain.ServiceException
import com.github.rain1208.sakenomi.repository.PaymentRepository

data class PartyPayments(
    val party: Party,
    val payments: List<Payment>,
    val total: Long,
)

class PaymentService(
    private val partyService: PartyService,
    private val paymentRepository: PaymentRepository,
) {

    /** 受領記録の登録(§10.7)。割り勘計算は行わず受領実績のみ保持する */
    fun add(event: Event, orderNo: Int, payerUserId: Long, amount: Long, recordedBy: Long): PartyPayments {
        if (amount <= 0) {
            throw ServiceException("金額は 1 円以上で指定してください")
        }
        val party = partyService.findByOrder(event, orderNo)
        paymentRepository.insert(party.id, payerUserId, amount, recordedBy)
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
