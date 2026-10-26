package com.github.rain1208.sakenomi.service

import com.github.rain1208.sakenomi.domain.Event
import com.github.rain1208.sakenomi.domain.Guest
import com.github.rain1208.sakenomi.repository.GuildSettingsRepository
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageEmbed
import java.time.Instant

/** log-channel への監査ログ出力(§11) */
class AuditLogger(
    private val settingsRepository: GuildSettingsRepository,
) {

    fun eventDeleted(guild: Guild, event: Event, executor: Member) {
        send(
            guild,
            EmbedBuilder()
                .setTitle("🗑️ イベント削除")
                .addField("イベント", "第${event.number}回 ${event.name}", false)
                .addField("実行者", executor.asMention, true)
                .setColor(0xE74C3C)
                .setTimestamp(Instant.now())
                .build(),
        )
    }

    fun capacityWarning(guild: Guild, categoryName: String, channelCount: Int) {
        send(
            guild,
            EmbedBuilder()
                .setTitle("⚠️ カテゴリ上限警告")
                .addField("対象カテゴリ", categoryName, true)
                .addField("現在チャンネル数", "$channelCount / 50", true)
                .setColor(0xE67E22)
                .setTimestamp(Instant.now())
                .build(),
        )
    }

    fun renumbered(guild: Guild, event: Event, oldNumber: Int, newNumber: Int, executor: Member) {
        send(
            guild,
            EmbedBuilder()
                .setTitle("🔢 手動採番修正")
                .addField("イベント", event.name, false)
                .addField("番号", "第${oldNumber}回 → 第${newNumber}回", true)
                .addField("実行者", executor.asMention, true)
                .setColor(0x9B59B6)
                .setTimestamp(Instant.now())
                .build(),
        )
    }

    fun organizerChanged(guild: Guild, event: Event, oldOrganizerUserId: Long, newOrganizerUserId: Long, executor: Member) {
        send(
            guild,
            EmbedBuilder()
                .setTitle("🔄 幹事変更")
                .addField("イベント", "第${event.number}回 ${event.name}", false)
                .addField("旧幹事", "<@$oldOrganizerUserId>", true)
                .addField("新幹事", "<@$newOrganizerUserId>", true)
                .addField("実行者", executor.asMention, true)
                .setColor(0x3498DB)
                .setTimestamp(Instant.now())
                .build(),
        )
    }

    fun participantAdded(guild: Guild, event: Event, target: Member, executor: Member) {
        send(
            guild,
            EmbedBuilder()
                .setTitle("➕ 参加者追加")
                .addField("イベント", "第${event.number}回 ${event.name}", false)
                .addField("対象ユーザー", target.asMention, true)
                .addField("実行者", executor.asMention, true)
                .setColor(0x2ECC71)
                .setTimestamp(Instant.now())
                .build(),
        )
    }

    fun participantRemoved(guild: Guild, event: Event, targetUserId: Long, executor: Member) {
        send(
            guild,
            EmbedBuilder()
                .setTitle("➖ 参加者削除")
                .addField("イベント", "第${event.number}回 ${event.name}", false)
                .addField("対象ユーザー", "<@$targetUserId>", true)
                .addField("実行者", executor.asMention, true)
                .setColor(0xE74C3C)
                .setTimestamp(Instant.now())
                .build(),
        )
    }

    fun guestAdded(guild: Guild, event: Event, guest: Guest, executor: Member, restored: Boolean) {
        send(
            guild,
            EmbedBuilder()
                .setTitle(if (restored) "➕ ゲスト追加(復元)" else "➕ ゲスト追加")
                .addField("イベント", "第${event.number}回 ${event.name}", false)
                .addField("ゲスト", guest.name, true)
                .addField("実行者", executor.asMention, true)
                .setColor(0x2ECC71)
                .setTimestamp(Instant.now())
                .build(),
        )
    }

    fun guestRemoved(guild: Guild, event: Event, guest: Guest, executor: Member) {
        send(
            guild,
            EmbedBuilder()
                .setTitle("➖ ゲスト削除")
                .addField("イベント", "第${event.number}回 ${event.name}", false)
                .addField("ゲスト", guest.name, true)
                .addField("実行者", executor.asMention, true)
                .setColor(0xE74C3C)
                .setTimestamp(Instant.now())
                .build(),
        )
    }

    fun guestRenamed(guild: Guild, event: Event, oldName: String, guest: Guest, executor: Member) {
        send(
            guild,
            EmbedBuilder()
                .setTitle("✏️ ゲスト名変更")
                .addField("イベント", "第${event.number}回 ${event.name}", false)
                .addField("名前", "$oldName → ${guest.name}", true)
                .addField("実行者", executor.asMention, true)
                .setColor(0x3498DB)
                .setTimestamp(Instant.now())
                .build(),
        )
    }

    private fun send(guild: Guild, embed: MessageEmbed) {
        val logChannelId = settingsRepository.find(guild.idLong).logChannelId ?: return
        guild.getTextChannelById(logChannelId)
            ?.sendMessageEmbeds(embed)
            ?.queue({}, {})
    }
}
