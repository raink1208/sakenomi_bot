package com.github.rain1208.sakenomi.repository

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class ParticipantRepository {

    fun isParticipant(eventId: Long, userId: Long): Boolean = transaction {
        !EventParticipantsTable.selectAll()
            .where { (EventParticipantsTable.eventId eq eventId) and (EventParticipantsTable.userId eq userId) }
            .empty()
    }

    fun add(eventId: Long, userId: Long) {
        transaction {
            EventParticipantsTable.insert {
                it[EventParticipantsTable.eventId] = eventId
                it[EventParticipantsTable.userId] = userId
            }
        }
    }

    fun remove(eventId: Long, userId: Long): Boolean = transaction {
        EventParticipantsTable.deleteWhere {
            (EventParticipantsTable.eventId eq eventId) and (EventParticipantsTable.userId eq userId)
        } > 0
    }

    fun countByEvent(eventId: Long): Long = transaction {
        EventParticipantsTable.selectAll()
            .where { EventParticipantsTable.eventId eq eventId }
            .count()
    }

    /** マイページ用: ユーザーが参加しているイベント ID 一覧 */
    fun listEventIdsByUser(userId: Long): List<Long> = transaction {
        EventParticipantsTable.selectAll()
            .where { EventParticipantsTable.userId eq userId }
            .map { it[EventParticipantsTable.eventId] }
    }

    fun listUserIdsByEvent(eventId: Long): List<Long> = transaction {
        EventParticipantsTable.selectAll()
            .where { EventParticipantsTable.eventId eq eventId }
            .map { it[EventParticipantsTable.userId] }
    }
}
