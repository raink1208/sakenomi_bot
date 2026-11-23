package com.github.rain1208.sakenomi.repository

import com.github.rain1208.sakenomi.domain.Oshi
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class OshiRepository {

    fun insert(guildId: Long, roleId: Long, name: String): Oshi = transaction {
        val id = OshiRolesTable.insert {
            it[OshiRolesTable.guildId] = guildId
            it[OshiRolesTable.roleId] = roleId
            it[OshiRolesTable.name] = name
        }[OshiRolesTable.id]
        Oshi(id, guildId, roleId, name)
    }

    /** 登録順 */
    fun listByGuild(guildId: Long): List<Oshi> = transaction {
        OshiRolesTable.selectAll()
            .where { OshiRolesTable.guildId eq guildId }
            .orderBy(OshiRolesTable.id)
            .map { it.toOshi() }
    }

    fun findByName(guildId: Long, name: String): Oshi? = transaction {
        OshiRolesTable.selectAll()
            .where { (OshiRolesTable.guildId eq guildId) and (OshiRolesTable.name eq name) }
            .firstOrNull()
            ?.toOshi()
    }

    fun findByRoleId(guildId: Long, roleId: Long): Oshi? = transaction {
        OshiRolesTable.selectAll()
            .where { (OshiRolesTable.guildId eq guildId) and (OshiRolesTable.roleId eq roleId) }
            .firstOrNull()
            ?.toOshi()
    }

    fun count(guildId: Long): Long = transaction {
        OshiRolesTable.selectAll()
            .where { OshiRolesTable.guildId eq guildId }
            .count()
    }

    fun delete(id: Long) {
        transaction {
            OshiRolesTable.deleteWhere { OshiRolesTable.id eq id }
        }
    }

    private fun ResultRow.toOshi() = Oshi(
        id = this[OshiRolesTable.id],
        guildId = this[OshiRolesTable.guildId],
        roleId = this[OshiRolesTable.roleId],
        name = this[OshiRolesTable.name],
    )
}
