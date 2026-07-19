package com.github.rain1208.sakenomi.discord.command

import com.github.rain1208.sakenomi.domain.ServiceException
import com.github.rain1208.sakenomi.domain.SettingKey
import com.github.rain1208.sakenomi.repository.EventRepository
import com.github.rain1208.sakenomi.service.SettingsService
import com.github.rain1208.sakenomi.util.Permissions
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class SettingsCommandHandler(
    private val settingsService: SettingsService,
    private val eventRepository: EventRepository,
) {

    fun handle(event: SlashCommandInteractionEvent) {
        val member = event.member!!
        val settings = settingsService.get(event.guild!!.idLong)
        if (!Permissions.isAdmin(member, settings)) {
            throw ServiceException("このコマンドは運営ロールまたはサーバー管理権限が必要です")
        }
        when (event.subcommandName) {
            "set" -> set(event)
            "show" -> show(event)
            "next-number" -> setNextNumber(event)
        }
    }

    private fun set(event: SlashCommandInteractionEvent) {
        val guildId = event.guild!!.idLong
        val keyStr = event.getOption("key")!!.asString
        val key = SettingKey.from(keyStr)
            ?: throw ServiceException("不明な設定キーです: $keyStr")

        val value: Long = when (key) {
            SettingKey.ADMIN_ROLE -> {
                val role = event.getOption("role")?.asRole
                    ?: throw ServiceException("`role` オプションでロールを指定してください")
                role.idLong
            }
            SettingKey.EVENT_CATEGORY, SettingKey.ARCHIVE_CATEGORY -> {
                val channel = event.getOption("channel")?.asChannel
                    ?: throw ServiceException("`channel` オプションでカテゴリを指定してください")
                if (channel.type != ChannelType.CATEGORY) {
                    throw ServiceException("`${key.key}` にはカテゴリを指定してください")
                }
                channel.idLong
            }
            SettingKey.LOG_CHANNEL, SettingKey.RECRUIT_CHANNEL -> {
                val channel = event.getOption("channel")?.asChannel
                    ?: throw ServiceException("`channel` オプションでテキストチャンネルを指定してください")
                if (channel.type != ChannelType.TEXT) {
                    throw ServiceException("`${key.key}` にはテキストチャンネルを指定してください")
                }
                channel.idLong
            }
        }

        settingsService.set(guildId, key, value)
        val remaining = if (settingsService.get(guildId).isComplete) {
            "初期設定は完了しています。"
        } else {
            "`/settings show` で残りの未設定項目を確認してください。"
        }
        event.reply("✅ `${key.key}` を設定しました。$remaining").setEphemeral(true).queue()
    }

    private fun show(event: SlashCommandInteractionEvent) {
        val guildId = event.guild!!.idLong
        val settings = settingsService.get(guildId)
        val nextNumber = eventRepository.getNextNumber(guildId)
        val embed = EmbedBuilder()
            .setTitle("⚙️ 現在の設定")
            .addField(SettingKey.EVENT_CATEGORY.label, formatChannel(settings.eventCategoryId), true)
            .addField(SettingKey.ARCHIVE_CATEGORY.label, formatChannel(settings.archiveCategoryId), true)
            .addField(SettingKey.ADMIN_ROLE.label, formatRole(settings.adminRoleId), true)
            .addField(SettingKey.LOG_CHANNEL.label, formatChannel(settings.logChannelId), true)
            .addField(SettingKey.RECRUIT_CHANNEL.label, formatChannel(settings.recruitChannelId), true)
            .addField("次回イベント番号", "第${nextNumber}回", true)
            .setFooter(if (settings.isComplete) "初期設定完了" else "初期設定が未完了です")
            .setColor(0x2ECC71)
            .build()
        event.replyEmbeds(embed).setEphemeral(true).queue()
    }

    /** 既存サーバー導入時などに次回採番を手動設定する(§7) */
    private fun setNextNumber(event: SlashCommandInteractionEvent) {
        val guildId = event.guild!!.idLong
        val number = event.getOption("number")!!.asInt
        val maxNumber = eventRepository.maxNumber(guildId)
        if (maxNumber != null && number <= maxNumber) {
            throw ServiceException("記録済みの最新イベントは第${maxNumber}回です。それより大きい番号を指定してください")
        }
        eventRepository.setNextNumber(guildId, number)
        event.reply("✅ 次回のイベントは「第${number}回」として作成されます。").setEphemeral(true).queue()
    }

    private fun formatChannel(id: Long?): String = id?.let { "<#$it>" } ?: "未設定"

    private fun formatRole(id: Long?): String = id?.let { "<@&$it>" } ?: "未設定"
}
