package com.github.rain1208.sakenomi.discord

import com.github.rain1208.sakenomi.domain.SettingKey
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData

/** スラッシュコマンド定義(§9.1)。グローバル登録 + guild_id 分離(§4) */
object CommandRegistry {

    fun commands(): List<SlashCommandData> = listOf(settings(), event(), party(), payment())

    private fun settings() = Commands.slash("settings", "Bot の設定を管理します(運営のみ)")
        .setContexts(InteractionContextType.GUILD)
        .addSubcommands(
            SubcommandData("set", "設定項目を登録・変更します")
                .addOptions(
                    OptionData(OptionType.STRING, "key", "設定キー", true)
                        .also { opt -> SettingKey.entries.forEach { opt.addChoice("${it.key} (${it.label})", it.key) } },
                    OptionData(OptionType.CHANNEL, "channel", "チャンネル / カテゴリ(チャンネル系キーの場合)", false),
                    OptionData(OptionType.ROLE, "role", "ロール(admin-role の場合)", false),
                ),
            SubcommandData("show", "現在の設定を表示します"),
            SubcommandData("next-number", "次に作成するイベントの「第n回」番号を設定します(既存サーバー導入時などに使用)")
                .addOptions(
                    OptionData(OptionType.INTEGER, "number", "次のイベントに割り当てる番号", true).setMinValue(1),
                ),
        )

    private fun event() = Commands.slash("event", "飲み会イベントを管理します")
        .setContexts(InteractionContextType.GUILD)
        .addSubcommands(
            SubcommandData("create", "イベントを作成します(専用チャンネル・募集メッセージを自動生成)")
                .addOptions(
                    OptionData(OptionType.STRING, "name", "イベント名", true).setRequiredLength(1, 80),
                    OptionData(OptionType.STRING, "area", "開催エリア", true).setRequiredLength(1, 80),
                    OptionData(OptionType.STRING, "date", "開催日(YYYY-MM-DD)", false),
                ),
            SubcommandData("edit", "イベント情報を編集します(イベントチャンネル内で実行)")
                .addOptions(
                    OptionData(OptionType.STRING, "name", "イベント名", false).setRequiredLength(1, 80),
                    OptionData(OptionType.STRING, "area", "開催エリア", false).setRequiredLength(1, 80),
                    OptionData(OptionType.STRING, "date", "開催日(YYYY-MM-DD)", false),
                ),
            SubcommandData("archive", "イベントをアーカイブ化します(イベントチャンネル内で実行)"),
            SubcommandData(
                "transfer-organizer",
                "幹事を変更します(幹事/運営・イベントチャンネル内で実行)",
            )
                .addOptions(
                    OptionData(OptionType.USER, "user", "新しい幹事", true),
                ),
            SubcommandData(
                "resend",
                "募集メッセージを募集チャンネルに再送信します(幹事/運営・イベントチャンネル内で実行)",
            ),
            SubcommandData("delete", "イベントを削除します(運営のみ・イベントチャンネル内で実行)"),
            SubcommandData("renumber", "第n回の番号を修正します(運営のみ・イベントチャンネル内で実行)")
                .addOptions(
                    OptionData(OptionType.INTEGER, "number", "新しい番号", true).setMinValue(1),
                ),
            SubcommandData("add-user", "参加者を追加します(運営のみ・イベントチャンネル内で実行)")
                .addOptions(
                    OptionData(OptionType.USER, "user", "追加するユーザー", true),
                ),
            SubcommandData("remove-user", "参加者を削除します(運営のみ・イベントチャンネル内で実行)")
                .addOptions(
                    OptionData(OptionType.USER, "user", "削除するユーザー", true),
                ),
        )

    private fun party() = Commands.slash("party", "飲み会(お店)を管理します(イベントチャンネル内で実行)")
        .setContexts(InteractionContextType.GUILD)
        .addSubcommands(
            SubcommandData("add", "お店を登録します(幹事/運営)")
                .addOptions(
                    OptionData(OptionType.STRING, "shop", "店名", true).setRequiredLength(1, 120),
                    OptionData(OptionType.INTEGER, "order", "n軒目(省略時は自動採番)", false).setMinValue(1),
                    OptionData(OptionType.INTEGER, "amount", "会計金額(円)", false).setMinValue(0),
                    OptionData(OptionType.STRING, "note", "メモ", false).setRequiredLength(1, 255),
                ),
            SubcommandData("list", "お店一覧を表示します"),
        )

    private fun payment() = Commands.slash("payment", "支払受領記録を管理します(幹事/運営・イベントチャンネル内で実行)")
        .setContexts(InteractionContextType.GUILD)
        .addSubcommands(
            SubcommandData("add", "参加者から受け取った金額を記録します")
                .addOptions(
                    OptionData(OptionType.INTEGER, "party", "お店の番号(n軒目)", true).setMinValue(1),
                    OptionData(OptionType.USER, "payer", "支払った参加者", true),
                    OptionData(OptionType.INTEGER, "amount", "受領金額(円)", true).setMinValue(0),
                ),
            SubcommandData("list", "受領記録の一覧と合計を表示します")
                .addOptions(
                    OptionData(OptionType.INTEGER, "party", "お店の番号(n軒目)", false).setMinValue(1),
                ),
        )
}
