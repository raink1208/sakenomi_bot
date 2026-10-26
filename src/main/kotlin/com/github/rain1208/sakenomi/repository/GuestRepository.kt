package com.github.rain1208.sakenomi.repository

import com.github.rain1208.sakenomi.domain.Guest
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.OffsetDateTime

class GuestRepository {

    fun insert(eventId: Long, name: String, createdBy: Long): Guest = transaction {
        val id = EventGuestsTable.insert {
            it[EventGuestsTable.eventId] = eventId
            it[EventGuestsTable.name] = name
            it[EventGuestsTable.createdBy] = createdBy
        }[EventGuestsTable.id]
        Guest(id, eventId, name, null)
    }

    fun findById(id: Long): Guest? = transaction {
        EventGuestsTable.selectAll()
            .where { EventGuestsTable.id eq id }
            .firstOrNull()
            ?.toGuest()
    }

    /** 削除済みも含めて名前で検索する(同名の再追加を復活として扱うため) */
    fun findByName(eventId: Long, name: String): Guest? = transaction {
        EventGuestsTable.selectAll()
            .where { (EventGuestsTable.eventId eq eventId) and (EventGuestsTable.name eq name) }
            .firstOrNull()
            ?.toGuest()
    }

    /** 削除されていないゲスト(登録順) */
    fun listActiveByEvent(eventId: Long): List<Guest> = transaction {
        EventGuestsTable.selectAll()
            .where { (EventGuestsTable.eventId eq eventId) and EventGuestsTable.deletedAt.isNull() }
            .orderBy(EventGuestsTable.id)
            .map { it.toGuest() }
    }

    fun softDelete(id: Long) {
        transaction {
            EventGuestsTable.update({ EventGuestsTable.id eq id }) {
                it[deletedAt] = OffsetDateTime.now()
            }
        }
    }

    fun restore(id: Long) {
        transaction {
            EventGuestsTable.update({ EventGuestsTable.id eq id }) {
                it[deletedAt] = null
            }
        }
    }

    fun rename(id: Long, name: String) {
        transaction {
            EventGuestsTable.update({ EventGuestsTable.id eq id }) {
                it[EventGuestsTable.name] = name
            }
        }
    }

    private fun ResultRow.toGuest() = Guest(
        id = this[EventGuestsTable.id],
        eventId = this[EventGuestsTable.eventId],
        name = this[EventGuestsTable.name],
        deletedAt = this[EventGuestsTable.deletedAt],
    )
}
