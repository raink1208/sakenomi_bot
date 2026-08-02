package com.github.rain1208.sakenomi.repository

import com.github.rain1208.sakenomi.domain.Event
import com.github.rain1208.sakenomi.domain.EventStatus
import org.jetbrains.exposed.sql.LongColumnType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.StatementType
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsert
import java.time.LocalDate
import java.time.OffsetDateTime

class EventRepository {

    /** event_counters を用いたアトミック採番(§7) */
    fun assignNumber(guildId: Long): Int = transaction {
        exec(
            stmt = """
                INSERT INTO event_counters (guild_id, next_number) VALUES (?, 2)
                ON CONFLICT (guild_id) DO UPDATE SET next_number = event_counters.next_number + 1
                RETURNING next_number - 1 AS assigned_number
            """.trimIndent(),
            args = listOf(LongColumnType() to guildId),
            explicitStatementType = StatementType.EXEC,
        ) { rs -> if (rs.next()) rs.getInt("assigned_number") else null }
            ?: error("イベント番号の採番に失敗しました")
    }

    fun insert(
        guildId: Long,
        number: Int,
        name: String,
        area: String,
        eventDate: LocalDate?,
        channelId: Long,
        roleId: Long,
        organizerUserId: Long,
    ): Long = transaction {
        EventsTable.insert {
            it[EventsTable.guildId] = guildId
            it[EventsTable.number] = number
            it[EventsTable.name] = name
            it[EventsTable.area] = area
            it[EventsTable.eventDate] = eventDate
            it[status] = EventStatus.RECRUITING.name
            it[EventsTable.channelId] = channelId
            it[EventsTable.roleId] = roleId
            it[EventsTable.organizerUserId] = organizerUserId
        }[EventsTable.id]
    }

    fun findById(id: Long): Event? = transaction {
        EventsTable.selectAll()
            .where { EventsTable.id eq id }
            .singleOrNull()
            ?.toEvent()
    }

    fun findByChannel(guildId: Long, channelId: Long): Event? = transaction {
        EventsTable.selectAll()
            .where { (EventsTable.guildId eq guildId) and (EventsTable.channelId eq channelId) }
            .singleOrNull()
            ?.toEvent()
    }

    /** Web コンソールのイベント一覧(募集中 / アーカイブ) */
    fun listByGuild(guildId: Long, status: EventStatus): List<Event> = transaction {
        EventsTable.selectAll()
            .where { (EventsTable.guildId eq guildId) and (EventsTable.status eq status.name) }
            .orderBy(EventsTable.number, org.jetbrains.exposed.sql.SortOrder.DESC)
            .map { it.toEvent() }
    }

    fun numberExists(guildId: Long, number: Int): Boolean = transaction {
        !EventsTable.selectAll()
            .where { (EventsTable.guildId eq guildId) and (EventsTable.number eq number) }
            .empty()
    }

    /** 記録済みイベントの最大番号(既存イベントが無ければ null) */
    fun maxNumber(guildId: Long): Int? = transaction {
        EventsTable.selectAll()
            .where { EventsTable.guildId eq guildId }
            .orderBy(EventsTable.number, org.jetbrains.exposed.sql.SortOrder.DESC)
            .limit(1)
            .map { it[EventsTable.number] }
            .singleOrNull()
    }

    /** 次回採番される番号(未設定なら 1) */
    fun getNextNumber(guildId: Long): Int = transaction {
        EventCountersTable.selectAll()
            .where { EventCountersTable.guildId eq guildId }
            .singleOrNull()
            ?.get(EventCountersTable.nextNumber)
            ?: 1
    }

    /**
     * 次回採番される番号を手動設定する(後からbotを導入したサーバー向け §7)。
     * 既存イベントの最大番号以下は renumber との整合性が崩れるため呼び出し側で検証する。
     */
    fun setNextNumber(guildId: Long, nextNumber: Int) {
        transaction {
            EventCountersTable.upsert(EventCountersTable.guildId) {
                it[EventCountersTable.guildId] = guildId
                it[EventCountersTable.nextNumber] = nextNumber
            }
        }
    }

    fun updateRecruitMessageId(id: Long, messageId: Long) {
        transaction {
            EventsTable.update({ EventsTable.id eq id }) {
                it[recruitMessageId] = messageId
            }
        }
    }

    fun updateInfo(id: Long, name: String, area: String, eventDate: LocalDate?) {
        transaction {
            EventsTable.update({ EventsTable.id eq id }) {
                it[EventsTable.name] = name
                it[EventsTable.area] = area
                it[EventsTable.eventDate] = eventDate
            }
        }
    }

    fun updateOrganizer(id: Long, organizerUserId: Long) {
        transaction {
            EventsTable.update({ EventsTable.id eq id }) {
                it[EventsTable.organizerUserId] = organizerUserId
            }
        }
    }

    fun updateNumber(id: Long, number: Int) {
        transaction {
            EventsTable.update({ EventsTable.id eq id }) {
                it[EventsTable.number] = number
            }
        }
    }

    fun markArchived(id: Long) {
        transaction {
            EventsTable.update({ EventsTable.id eq id }) {
                it[status] = EventStatus.ARCHIVED.name
                it[archivedAt] = OffsetDateTime.now()
            }
        }
    }

    fun delete(id: Long) {
        transaction {
            EventsTable.deleteWhere { EventsTable.id eq id }
        }
    }

    private fun ResultRow.toEvent() = Event(
        id = this[EventsTable.id],
        guildId = this[EventsTable.guildId],
        number = this[EventsTable.number],
        name = this[EventsTable.name],
        area = this[EventsTable.area],
        eventDate = this[EventsTable.eventDate],
        status = EventStatus.valueOf(this[EventsTable.status]),
        channelId = this[EventsTable.channelId],
        roleId = this[EventsTable.roleId],
        recruitMessageId = this[EventsTable.recruitMessageId],
        organizerUserId = this[EventsTable.organizerUserId],
    )
}
