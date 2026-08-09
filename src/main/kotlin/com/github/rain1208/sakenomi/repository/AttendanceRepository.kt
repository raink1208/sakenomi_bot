package com.github.rain1208.sakenomi.repository

import com.github.rain1208.sakenomi.domain.Attendance
import com.github.rain1208.sakenomi.domain.AttendanceStatus
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import java.time.OffsetDateTime

class AttendanceRepository {

    /** party_id + user_id の一意制約で upsert する(未確認→確認済みへの更新も同じ経路) */
    fun upsert(partyId: Long, userId: Long, status: AttendanceStatus, recordedBy: Long) {
        transaction {
            AttendancesTable.upsert(AttendancesTable.partyId, AttendancesTable.userId) {
                it[AttendancesTable.partyId] = partyId
                it[AttendancesTable.userId] = userId
                it[AttendancesTable.status] = status.name
                it[AttendancesTable.recordedBy] = recordedBy
                it[recordedAt] = OffsetDateTime.now()
            }
        }
    }

    fun listByParty(partyId: Long): List<Attendance> = transaction {
        AttendancesTable.selectAll()
            .where { AttendancesTable.partyId eq partyId }
            .map { it.toAttendance() }
    }

    /** 未確認に戻す */
    fun delete(partyId: Long, userId: Long): Boolean = transaction {
        AttendancesTable.deleteWhere {
            (AttendancesTable.partyId eq partyId) and (AttendancesTable.userId eq userId)
        } > 0
    }

    private fun ResultRow.toAttendance() = Attendance(
        id = this[AttendancesTable.id],
        partyId = this[AttendancesTable.partyId],
        userId = this[AttendancesTable.userId],
        status = AttendanceStatus.valueOf(this[AttendancesTable.status]),
        recordedBy = this[AttendancesTable.recordedBy],
        recordedAt = this[AttendancesTable.recordedAt],
    )
}
