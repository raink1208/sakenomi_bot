package com.github.rain1208.sakenomi.util

import com.github.rain1208.sakenomi.domain.Event
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import java.time.format.DateTimeFormatter

object EventEmbeds {

    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

    private const val COLOR_RECRUIT = 0xF1C40F
    private const val COLOR_CHANNEL = 0x3498DB

    fun formatDate(event: Event): String = event.eventDate?.format(DATE_FORMAT) ?: "未定"

    fun formatYen(amount: Long): String = "%,d円".format(amount)

    /** 募集チャンネルに投稿する募集メッセージ */
    fun recruit(event: Event): MessageEmbed = EmbedBuilder()
        .setTitle("🍻 第${event.number}回 ${event.name}")
        .setDescription("下の「参加する」ボタンを押すと参加登録され、専用チャンネルが見えるようになります。")
        .addField("開催エリア", event.area, true)
        .addField("開催日", formatDate(event), true)
        .addField("幹事", "<@${event.organizerUserId}>", true)
        .setColor(COLOR_RECRUIT)
        .build()

    /** イベントチャンネルの案内メッセージ */
    fun welcome(event: Event): MessageEmbed = EmbedBuilder()
        .setTitle("🍻 第${event.number}回 ${event.name}")
        .setDescription("このチャンネルはイベント参加者専用です。\n参加をキャンセルする場合は下のボタンを押してください。")
        .addField("開催エリア", event.area, true)
        .addField("開催日", formatDate(event), true)
        .addField("幹事", "<@${event.organizerUserId}>", true)
        .setColor(COLOR_CHANNEL)
        .build()
}
