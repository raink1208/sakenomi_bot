package com.github.rain1208.sakenomi.config

import com.typesafe.config.ConfigFactory

data class AppConfig(
    val discordToken: String,
    val dbUrl: String,
    val dbUser: String,
    val dbPassword: String,
    val channelWarnThreshold: Int,
    /** 設定時はグローバル登録ではなくこのギルドにコマンドを即時反映する(開発用) */
    val developmentGuildId: Long?,
    val discordClientId: String,
    val discordClientSecret: String,
    val publicBaseUrl: String,
    val sessionEncryptKey: String,
    val sessionSignKey: String,
    val webPort: Int,
) {
    companion object {
        fun load(): AppConfig {
            val config = ConfigFactory.load()
            return AppConfig(
                discordToken = config.getString("discord.token"),
                dbUrl = config.getString("db.url"),
                dbUser = config.getString("db.user"),
                dbPassword = config.getString("db.password"),
                channelWarnThreshold = config.getInt("bot.channel-warn-threshold"),
                developmentGuildId = if (config.hasPath("bot.development-guild")) {
                    config.getString("bot.development-guild").toLongOrNull()
                } else {
                    null
                },
                discordClientId = config.getString("web.discord-client-id"),
                discordClientSecret = config.getString("web.discord-client-secret"),
                publicBaseUrl = config.getString("web.public-base-url"),
                sessionEncryptKey = config.getString("web.session-encrypt-key"),
                sessionSignKey = config.getString("web.session-sign-key"),
                webPort = config.getInt("web.port"),
            )
        }
    }
}