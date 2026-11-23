package com.github.rain1208.sakenomi.discord.component

import com.github.rain1208.sakenomi.domain.ServiceException
import com.github.rain1208.sakenomi.service.OshiService
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu

/**
 * 推し選択パネルのコンポーネント
 * - oshi:open        … パネルのボタン。実行者だけに見えるセレクトメニューを表示する
 * - oshi:select:{n}  … 推しのセレクトメニュー(25件ごとに分割、最大4つ)
 * - oshi:apply       … 決定ボタン。選択内容をロールに反映する
 *
 * 選択途中の状態はサーバー側に持たず、セレクト操作のたびにメニューのデフォルト値として
 * メッセージへ書き戻す。決定時はメッセージ上のメニューから選択肢・選択値を読み取るため、
 * Bot の再起動を挟んでも操作を継続できる
 */
class OshiComponentHandler(
    private val oshiService: OshiService,
) {

    fun handleButton(event: ButtonInteractionEvent) {
        when (event.componentId) {
            OPEN_ID -> open(event)
            APPLY_ID -> apply(event)
        }
    }

    fun handleSelect(event: StringSelectInteractionEvent) {
        if (!event.componentId.startsWith(SELECT_ID_PREFIX)) return
        // 選択内容をデフォルト値として保持し、決定ボタン押下時に読み取る
        val updated = event.selectMenu.createCopy()
            .setDefaultValues(event.values)
            .build()
        event.editSelectMenu(updated).queue()
    }

    private fun open(event: ButtonInteractionEvent) {
        val guild = event.guild!!
        val available = oshiService.listAvailable(guild)
        if (available.isEmpty()) {
            throw ServiceException("選択できる推しがまだ登録されていません")
        }
        val currentRoleIds = event.member!!.roles.map { it.idLong }.toSet()
        val chunks = available.chunked(OshiService.OPTIONS_PER_MENU).take(OshiService.MAX_MENUS)
        val menus = chunks.mapIndexed { index, chunk ->
            val placeholder = if (chunks.size > 1) "推しを選択(${index + 1}/${chunks.size})" else "推しを選択(複数選択可)"
            StringSelectMenu.create("$SELECT_ID_PREFIX$index")
                .setPlaceholder(placeholder)
                .setRequiredRange(0, chunk.size)
                .addOptions(
                    chunk.map { (oshi, role) ->
                        SelectOption.of(oshi.name, role.id).withDefault(role.idLong in currentRoleIds)
                    },
                )
                .build()
        }
        // ActionRow は Iterable のため `+ ActionRow` だと中身が展開されてしまう。listOf で包む
        val rows = menus.map { ActionRow.of(it) } + listOf(ActionRow.of(Button.success(APPLY_ID, "決定")))
        event.reply(
            "推しを選んで「決定」を押してください(複数選択できます)。\n" +
                "現在の推しは選択済みの状態で表示されています。選択を外した推しのロールは外れます。",
        )
            .setEphemeral(true)
            .setComponents(rows)
            .queue()
    }

    private fun apply(event: ButtonInteractionEvent) {
        val menus = event.message.actionRows
            .flatMap { it.components }
            .filterIsInstance<StringSelectMenu>()
            .filter { it.id?.startsWith(SELECT_ID_PREFIX) == true }
        val options = menus.flatMap { it.options }
        val offered = options.mapNotNull { it.value.toLongOrNull() }.toSet()
        val selected = options.filter { it.isDefault }.mapNotNull { it.value.toLongOrNull() }.toSet()

        event.deferEdit().queue()
        val result = oshiService.applySelection(event.guild!!, event.member!!, offered, selected)
        val message = if (result.added.isEmpty() && result.removed.isEmpty()) {
            "推しに変更はありませんでした"
        } else {
            buildString {
                append("✅ 推しを更新しました")
                if (result.added.isNotEmpty()) {
                    append("\n追加: ").append(result.added.joinToString("、") { it.name })
                }
                if (result.removed.isNotEmpty()) {
                    append("\n解除: ").append(result.removed.joinToString("、") { it.name })
                }
            }
        }
        event.hook.editOriginal(message).setComponents().queue()
    }

    companion object {
        const val OPEN_ID = "oshi:open"
        const val APPLY_ID = "oshi:apply"
        const val SELECT_ID_PREFIX = "oshi:select:"
        const val COLOR = 0xFF69B4

        fun openButton(): Button = Button.primary(OPEN_ID, "推しを設定する").withEmoji(Emoji.fromUnicode("💖"))

        fun panelEmbed(): MessageEmbed = EmbedBuilder()
            .setTitle("💖 推しロール登録")
            .setDescription(
                "下のボタンを押すと推しを選択できます。\n" +
                    "選んだ推しのロールが付与されます(複数選択可)。\n" +
                    "推しの変更・解除も同じボタンから行えます。",
            )
            .setColor(COLOR)
            .build()
    }
}
