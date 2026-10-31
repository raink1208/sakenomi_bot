package com.github.rain1208.sakenomi

import com.github.rain1208.sakenomi.config.AppConfig
import com.github.rain1208.sakenomi.config.DatabaseFactory
import com.github.rain1208.sakenomi.config.appModule
import com.github.rain1208.sakenomi.discord.CommandRegistry
import com.github.rain1208.sakenomi.discord.DiscordGateway
import com.github.rain1208.sakenomi.discord.listener.InteractionListener
import com.github.rain1208.sakenomi.repository.EventRepository
import com.github.rain1208.sakenomi.repository.PartyRepository
import com.github.rain1208.sakenomi.repository.PaymentRepository
import com.github.rain1208.sakenomi.service.AttendanceService
import com.github.rain1208.sakenomi.service.EventService
import com.github.rain1208.sakenomi.service.GuestService
import com.github.rain1208.sakenomi.service.ParticipantService
import com.github.rain1208.sakenomi.service.PartyService
import com.github.rain1208.sakenomi.service.PaymentService
import com.github.rain1208.sakenomi.service.RosterService
import com.github.rain1208.sakenomi.service.SettingsService
import com.github.rain1208.sakenomi.web.startWebServer
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy
import org.koin.core.context.GlobalContext.startKoin
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("com.github.rain1208.sakenomi.Application")

    val config = AppConfig.load()
    require(config.discordToken.isNotBlank()) { "DISCORD_TOKEN が設定されていません" }

    // マイグレーション適用後に JDA を接続する(§14.3)
    DatabaseFactory.init(config)
    logger.info("データベース初期化完了")

    val koin = startKoin {
        modules(appModule(config))
    }.koin

    // Web コンソールの参加者追加(ギルドメンバー一覧取得)に GUILD_MEMBERS が必要(Discord Developer Portal で特権インテントの有効化が必須)
    // createLight はメンバーをキャッシュしないため、Web の各リクエストで Discord REST API への
    // メンバー取得が発生しレート制限待ちでページ表示が数秒遅れていた。起動時に全メンバーを
    // チャンク取得してキャッシュし、以後はゲートウェイイベントで最新状態に保つ
    val jda = JDABuilder.createLight(config.discordToken, GatewayIntent.GUILD_MEMBERS)
        .setMemberCachePolicy(MemberCachePolicy.ALL)
        .setChunkingFilter(ChunkingFilter.ALL)
        .addEventListeners(koin.get<InteractionListener>())
        .build()

    jda.awaitReady()

    // Web コンソールが Discord 情報を参照できるよう JDA を紐付ける(Web仕様書.md §1.3)
    koin.get<DiscordGateway>().attachJda(jda)

    startWebServer(
        config = config,
        gateway = koin.get(),
        settingsService = koin.get<SettingsService>(),
        eventService = koin.get<EventService>(),
        participantService = koin.get<ParticipantService>(),
        partyService = koin.get<PartyService>(),
        paymentService = koin.get<PaymentService>(),
        attendanceService = koin.get<AttendanceService>(),
        guestService = koin.get<GuestService>(),
        rosterService = koin.get<RosterService>(),
        partyRepository = koin.get<PartyRepository>(),
        paymentRepository = koin.get<PaymentRepository>(),
        eventRepository = koin.get<EventRepository>(),
    )

    // グローバルコマンド登録(本番用) + guild_id 分離(§4)
    jda.updateCommands().addCommands(CommandRegistry.commands()).queue {
        logger.info("グローバルコマンドを登録しました: {}", it.joinToString(", ") { cmd -> "/${cmd.name}" })
    }

    val developmentGuildId = config.developmentGuildId
    if (developmentGuildId != null) {
        // 開発用ギルドにはギルドコマンドとしても登録し即時反映させる(グローバル反映の待ち時間を回避)
        val guild = jda.getGuildById(developmentGuildId)
            ?: error("開発用ギルド(id=$developmentGuildId)が見つかりません。Bot が招待されているか確認してください")
        guild.updateCommands().addCommands(CommandRegistry.commands()).queue {
            logger.info(
                "開発用ギルド({})にコマンドを登録しました(即時反映): {}",
                developmentGuildId,
                it.joinToString(", ") { cmd -> "/${cmd.name}" },
            )
        }
    }
}
