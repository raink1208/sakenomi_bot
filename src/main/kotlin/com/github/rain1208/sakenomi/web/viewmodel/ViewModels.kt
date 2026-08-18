package com.github.rain1208.sakenomi.web.viewmodel

import com.github.rain1208.sakenomi.domain.AttendanceStatus
import com.github.rain1208.sakenomi.domain.Event
import com.github.rain1208.sakenomi.domain.Party
import com.github.rain1208.sakenomi.service.AttendanceEntry
import com.github.rain1208.sakenomi.util.EventEmbeds

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

    fun party(party: Party, total: Long? = null): Map<String, Any?> {
        val diff = if (party.totalAmount != null && total != null) total - party.totalAmount else null
        return mapOf(
            "id" to party.id,
            "eventId" to party.eventId,
            "orderNo" to party.orderNo,
            "shopName" to party.shopName,
            "totalAmount" to party.totalAmount,
            "totalAmountText" to (party.totalAmount?.let { EventEmbeds.formatYen(it) } ?: "未確定"),
            "note" to party.note,
            "receivedTotal" to total,
            "receivedTotalText" to total?.let { EventEmbeds.formatYen(it) },
            "diffText" to diff?.let { EventEmbeds.formatYen(it) },
            "diffPositive" to diff?.let { it >= 0 },
        )
    }

    fun attendanceEntry(entry: AttendanceEntry, name: String, receivedAmount: Long? = null): Map<String, Any?> = mapOf(
        "userId" to entry.userId,
        "name" to name,
        "status" to entry.status?.name,
        "statusLabel" to when (entry.status) {
            AttendanceStatus.PRESENT -> "出席"
            AttendanceStatus.ABSENT -> "欠席"
            null -> "未確認"
        },
        "receivedAmountText" to (receivedAmount?.let { EventEmbeds.formatYen(it) } ?: "-"),
        "receivedAmount" to receivedAmount,
    )
}
