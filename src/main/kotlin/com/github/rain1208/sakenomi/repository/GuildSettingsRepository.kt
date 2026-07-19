package com.github.rain1208.sakenomi.repository

import com.github.rain1208.sakenomi.domain.GuildSettings
import com.github.rain1208.sakenomi.domain.SettingKey
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import java.time.OffsetDateTime

class GuildSettingsRepository {

    fun find(guildId: Long): GuildSettings = transaction {
        GuildSettingsTable.selectAll()
            .where { GuildSettingsTable.guildId eq guildId }
            .singleOrNull()
            ?.toGuildSettings()
            ?: GuildSettings(guildId, null, null, null, null, null)
    }

    fun set(guildId: Long, key: SettingKey, value: Long) {
        transaction {
            GuildSettingsTable.upsert(GuildSettingsTable.guildId) {
                it[GuildSettingsTable.guildId] = guildId
                it[columnFor(key)] = value
                it[updatedAt] = OffsetDateTime.now()
            }
        }
    }

    private fun columnFor(key: SettingKey): Column<Long?> = when (key) {
        SettingKey.EVENT_CATEGORY -> GuildSettingsTable.eventCategoryId
        SettingKey.ARCHIVE_CATEGORY -> GuildSettingsTable.archiveCategoryId
        SettingKey.ADMIN_ROLE -> GuildSettingsTable.adminRoleId
        SettingKey.LOG_CHANNEL -> GuildSettingsTable.logChannelId
        SettingKey.RECRUIT_CHANNEL -> GuildSettingsTable.recruitChannelId
    }

    private fun ResultRow.toGuildSettings() = GuildSettings(
        guildId = this[GuildSettingsTable.guildId],
        eventCategoryId = this[GuildSettingsTable.eventCategoryId],
        archiveCategoryId = this[GuildSettingsTable.archiveCategoryId],
        adminRoleId = this[GuildSettingsTable.adminRoleId],
        logChannelId = this[GuildSettingsTable.logChannelId],
        recruitChannelId = this[GuildSettingsTable.recruitChannelId],
    )
}
