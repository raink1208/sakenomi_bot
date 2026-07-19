package com.github.rain1208.sakenomi.service

import com.github.rain1208.sakenomi.domain.GuildSettings
import com.github.rain1208.sakenomi.domain.ServiceException
import com.github.rain1208.sakenomi.domain.SettingKey
import com.github.rain1208.sakenomi.repository.GuildSettingsRepository

class SettingsService(
    private val settingsRepository: GuildSettingsRepository,
) {

    fun get(guildId: Long): GuildSettings = settingsRepository.find(guildId)

    fun set(guildId: Long, key: SettingKey, value: Long) {
        settingsRepository.set(guildId, key, value)
    }

    /** 初期設定完了チェック。未完了ならユーザー向けエラー */
    fun requireComplete(guildId: Long): GuildSettings {
        val settings = get(guildId)
        if (!settings.isComplete) {
            val missing = missingKeys(settings).joinToString(", ") { "`${it.key}`" }
            throw ServiceException("初期設定が未完了です。`/settings set` で以下を設定してください: $missing")
        }
        return settings
    }

    private fun missingKeys(settings: GuildSettings): List<SettingKey> = buildList {
        if (settings.eventCategoryId == null) add(SettingKey.EVENT_CATEGORY)
        if (settings.archiveCategoryId == null) add(SettingKey.ARCHIVE_CATEGORY)
        if (settings.adminRoleId == null) add(SettingKey.ADMIN_ROLE)
        if (settings.logChannelId == null) add(SettingKey.LOG_CHANNEL)
        if (settings.recruitChannelId == null) add(SettingKey.RECRUIT_CHANNEL)
    }
}
