package com.github.rain1208.sakenomi.web.viewmodel

import com.github.rain1208.sakenomi.domain.AttendanceStatus
import com.github.rain1208.sakenomi.domain.Event
import com.github.rain1208.sakenomi.domain.Party
import com.github.rain1208.sakenomi.domain.Payment
import com.github.rain1208.sakenomi.service.AttendanceEntry
import com.github.rain1208.sakenomi.util.EventEmbeds
import java.time.format.DateTimeFormatter

private val DATETIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")

/** FreeMarker テンプレートへ渡す表示用モデルへの変換 */
object ViewModels {

    fun event(event: Event, participantCount: Long? = null, organizerName: String? = null): Map<String, Any?> = mapOf(
        "id" to event.id,
        "number" to event.number,
        "name" to event.name,
        "area" to event.area,
        "date" to EventEmbeds.formatDate(event),
        "status" to event.status.name,
        "isRecruiting" to (event.status.name == "RECRUITING"),
        "channelId" to event.channelId,
        "organizerUserId" to event.organizerUserId,
        "organizerName" to (organizerName ?: event.organizerUserId.toString()),
        "participantCount" to participantCount,
    )

    fun party(party: Party, total: Long? = null): Map<String, Any?> = mapOf(
        "id" to party.id,
        "eventId" to party.eventId,
        "orderNo" to party.orderNo,
        "shopName" to party.shopName,
        "totalAmount" to party.totalAmount,
        "totalAmountText" to (party.totalAmount?.let { EventEmbeds.formatYen(it) } ?: "未確定"),
        "note" to party.note,
        "receivedTotal" to total,
        "receivedTotalText" to total?.let { EventEmbeds.formatYen(it) },
    )

    fun attendanceEntry(entry: AttendanceEntry, name: String): Map<String, Any?> = mapOf(
        "userId" to entry.userId,
        "name" to name,
        "status" to entry.status?.name,
        "statusLabel" to when (entry.status) {
            AttendanceStatus.PRESENT -> "出席"
            AttendanceStatus.ABSENT -> "欠席"
            null -> "未確認"
        },
    )

    fun payment(payment: Payment, payerName: String, recordedByName: String): Map<String, Any?> = mapOf(
        "id" to payment.id,
        "partyId" to payment.partyId,
        "payerUserId" to payment.payerUserId,
        "payerName" to payerName,
        "amount" to payment.amount,
        "amountText" to EventEmbeds.formatYen(payment.amount),
        "recordedByName" to recordedByName,
        "receivedAt" to payment.receivedAt.format(DATETIME_FORMAT),
    )
}
