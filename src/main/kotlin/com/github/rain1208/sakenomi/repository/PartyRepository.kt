package com.github.rain1208.sakenomi.repository

import com.github.rain1208.sakenomi.domain.Party
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class PartyRepository {

    fun insert(eventId: Long, orderNo: Int, shopName: String, totalAmount: Long?, note: String?): Party = transaction {
        val id = PartiesTable.insert {
            it[PartiesTable.eventId] = eventId
            it[PartiesTable.orderNo] = orderNo
            it[PartiesTable.shopName] = shopName
            it[PartiesTable.totalAmount] = totalAmount
            it[PartiesTable.note] = note
        }[PartiesTable.id]
        Party(id, eventId, orderNo, shopName, totalAmount, note)
    }

    fun nextOrderNo(eventId: Long): Int = transaction {
        val maxOrder = PartiesTable.orderNo.max()
        val current = PartiesTable.select(maxOrder)
            .where { PartiesTable.eventId eq eventId }
            .first()[maxOrder] ?: 0
        current + 1
    }

    fun existsOrder(eventId: Long, orderNo: Int): Boolean = transaction {
        !PartiesTable.selectAll()
            .where { (PartiesTable.eventId eq eventId) and (PartiesTable.orderNo eq orderNo) }
            .empty()
    }

    fun findByOrder(eventId: Long, orderNo: Int): Party? = transaction {
        PartiesTable.selectAll()
            .where { (PartiesTable.eventId eq eventId) and (PartiesTable.orderNo eq orderNo) }
            .singleOrNull()
            ?.toParty()
    }

    fun findById(id: Long): Party? = transaction {
        PartiesTable.selectAll()
            .where { PartiesTable.id eq id }
            .singleOrNull()
            ?.toParty()
    }

    fun updateInfo(id: Long, shopName: String, totalAmount: Long?, note: String?) {
        transaction {
            PartiesTable.update({ PartiesTable.id eq id }) {
                it[PartiesTable.shopName] = shopName
                it[PartiesTable.totalAmount] = totalAmount
                it[PartiesTable.note] = note
            }
        }
    }

    fun listByEvent(eventId: Long): List<Party> = transaction {
        PartiesTable.selectAll()
            .where { PartiesTable.eventId eq eventId }
            .orderBy(PartiesTable.orderNo)
            .map { it.toParty() }
    }

    private fun ResultRow.toParty() = Party(
        id = this[PartiesTable.id],
        eventId = this[PartiesTable.eventId],
        orderNo = this[PartiesTable.orderNo],
        shopName = this[PartiesTable.shopName],
        totalAmount = this[PartiesTable.totalAmount],
        note = this[PartiesTable.note],
    )
}
