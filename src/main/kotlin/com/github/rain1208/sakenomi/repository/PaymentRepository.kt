package com.github.rain1208.sakenomi.repository

import com.github.rain1208.sakenomi.domain.Payment
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import java.time.OffsetDateTime

class PaymentRepository {

    /** party_id + payer_user_id の一意制約で upsert する(同じ人への再登録は金額を上書き) */
    fun upsert(partyId: Long, payerUserId: Long, amount: Long, recordedBy: Long) {
        transaction {
            PaymentsTable.upsert(PaymentsTable.partyId, PaymentsTable.payerUserId) {
                it[PaymentsTable.partyId] = partyId
                it[PaymentsTable.payerUserId] = payerUserId
                it[PaymentsTable.amount] = amount
                it[PaymentsTable.recordedBy] = recordedBy
                it[receivedAt] = OffsetDateTime.now()
            }
        }
    }

    fun delete(partyId: Long, payerUserId: Long) {
        transaction {
            PaymentsTable.deleteWhere { (PaymentsTable.partyId eq partyId) and (PaymentsTable.payerUserId eq payerUserId) }
        }
    }

    fun listByParty(partyId: Long): List<Payment> = transaction {
        PaymentsTable.selectAll()
            .where { PaymentsTable.partyId eq partyId }
            .orderBy(PaymentsTable.receivedAt)
            .map { it.toPayment() }
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
