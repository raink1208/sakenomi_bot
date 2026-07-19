package com.github.rain1208.sakenomi.repository

import com.github.rain1208.sakenomi.domain.Payment
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime

class PaymentRepository {

    fun insert(partyId: Long, payerUserId: Long, amount: Long, recordedBy: Long) {
        transaction {
            PaymentsTable.insert {
                it[PaymentsTable.partyId] = partyId
                it[PaymentsTable.payerUserId] = payerUserId
                it[PaymentsTable.amount] = amount
                it[PaymentsTable.recordedBy] = recordedBy
                it[receivedAt] = OffsetDateTime.now()
            }
        }
    }

    fun listByParty(partyId: Long): List<Payment> = transaction {
        PaymentsTable.selectAll()
            .where { PaymentsTable.partyId eq partyId }
            .orderBy(PaymentsTable.receivedAt)
            .map { it.toPayment() }
    }

    fun totalByParty(partyId: Long): Long = transaction {
        val sumAmount = PaymentsTable.amount.sum()
        PaymentsTable.select(sumAmount)
            .where { PaymentsTable.partyId eq partyId }
            .first()[sumAmount] ?: 0L
    }

    /** マイページ用: 指定パーティ群における自分の受領記録(支払記録) */
    fun listByPayerAndParties(payerUserId: Long, partyIds: Collection<Long>): List<Payment> = transaction {
        if (partyIds.isEmpty()) return@transaction emptyList()
        PaymentsTable.selectAll()
            .where { (PaymentsTable.payerUserId eq payerUserId) and (PaymentsTable.partyId inList partyIds) }
            .orderBy(PaymentsTable.receivedAt)
            .map { it.toPayment() }
    }

    private fun ResultRow.toPayment() = Payment(
        id = this[PaymentsTable.id],
        partyId = this[PaymentsTable.partyId],
        payerUserId = this[PaymentsTable.payerUserId],
        amount = this[PaymentsTable.amount],
        recordedBy = this[PaymentsTable.recordedBy],
        receivedAt = this[PaymentsTable.receivedAt],
    )
}
