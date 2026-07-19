package com.github.rain1208.sakenomi.service

import com.github.rain1208.sakenomi.domain.Event
import com.github.rain1208.sakenomi.domain.Party
import com.github.rain1208.sakenomi.domain.ServiceException
import com.github.rain1208.sakenomi.repository.PartyRepository

class PartyService(
    private val partyRepository: PartyRepository,
) {

    /** 飲み会(お店)登録(§10.7)。order 未指定時は既存最大 +1 を自動採番 */
    fun add(event: Event, shopName: String, orderNo: Int?, totalAmount: Long?, note: String?): Party {
        if (totalAmount != null && totalAmount < 0) {
            throw ServiceException("会計金額は 0 以上で指定してください")
        }
        val order = orderNo ?: partyRepository.nextOrderNo(event.id)
        if (partyRepository.existsOrder(event.id, order)) {
            throw ServiceException("${order}軒目は既に登録されています")
        }
        return partyRepository.insert(event.id, order, shopName, totalAmount, note)
    }

    fun list(event: Event): List<Party> = partyRepository.listByEvent(event.id)

    fun findByOrder(event: Event, orderNo: Int): Party =
        partyRepository.findByOrder(event.id, orderNo)
            ?: throw ServiceException("${orderNo}軒目のお店は登録されていません。`/party list` で確認してください")
}
