package com.github.rain1208.sakenomi.discord.listener

import com.github.rain1208.sakenomi.discord.button.EventButtonHandler
import com.github.rain1208.sakenomi.discord.command.AttendanceCommandHandler
import com.github.rain1208.sakenomi.discord.command.EventCommandHandler
import com.github.rain1208.sakenomi.discord.command.PartyCommandHandler
import com.github.rain1208.sakenomi.discord.command.PaymentCommandHandler
import com.github.rain1208.sakenomi.discord.command.SettingsCommandHandler
import com.github.rain1208.sakenomi.domain.ServiceException
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import org.slf4j.LoggerFactory

class InteractionListener(
    private val settingsHandler: SettingsCommandHandler,
    private val eventHandler: EventCommandHandler,
    private val partyHandler: PartyCommandHandler,
    private val paymentHandler: PaymentCommandHandler,
    private val attendanceHandler: AttendanceCommandHandler,
    private val buttonHandler: EventButtonHandler,
) : ListenerAdapter() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun onReady(event: ReadyEvent) {
        logger.info("Bot 起動完了: {} (guilds={})", event.jda.selfUser.asTag, event.guildTotalCount)
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.guild == null || event.member == null) {
            event.reply("このコマンドはサーバー内でのみ実行できます").setEphemeral(true).queue()
            return
        }
        runCatchingInteraction(event) {
            when (event.name) {
                "settings" -> settingsHandler.handle(event)
                "event" -> eventHandler.handle(event)
                "party" -> partyHandler.handle(event)
                "payment" -> paymentHandler.handle(event)
                "attendance" -> attendanceHandler.handle(event)
            }
        }
    }

    override fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        if (event.guild == null || event.member == null) return
        try {
            when (event.name) {
                "event" -> eventHandler.autoComplete(event)
                else -> event.replyChoices(emptyList()).queue({}, {})
            }
        } catch (e: Exception) {
            // 補完の失敗は候補なしとして扱う(コマンド実行時に改めて検証される)
            logger.warn("オートコンプリート処理中にエラーが発生しました", e)
            event.replyChoices(emptyList()).queue({}, {})
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (event.guild == null || event.member == null) return
        if (!event.componentId.startsWith("event:")) return
        runCatchingInteraction(event) {
            buttonHandler.handle(event)
        }
    }

    private fun runCatchingInteraction(event: IReplyCallback, block: () -> Unit) {
        try {
            block()
        } catch (e: ServiceException) {
            replyError(event, e.message ?: "エラーが発生しました")
        } catch (e: Exception) {
            logger.error("インタラクション処理中に予期しないエラーが発生しました", e)
            replyError(event, "予期しないエラーが発生しました。時間をおいて再度お試しください")
        }
    }

    private fun replyError(event: IReplyCallback, message: String) {
        if (event.isAcknowledged) {
            event.hook.sendMessage("❌ $message").setEphemeral(true).queue({}, {})
        } else {
            event.reply("❌ $message").setEphemeral(true).queue({}, {})
        }
    }
}
