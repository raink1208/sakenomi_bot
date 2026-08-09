package com.github.rain1208.sakenomi.config

import com.github.rain1208.sakenomi.discord.DiscordGateway
import com.github.rain1208.sakenomi.discord.button.EventButtonHandler
import com.github.rain1208.sakenomi.discord.command.AttendanceCommandHandler
import com.github.rain1208.sakenomi.discord.command.EventCommandHandler
import com.github.rain1208.sakenomi.discord.command.PartyCommandHandler
import com.github.rain1208.sakenomi.discord.command.PaymentCommandHandler
import com.github.rain1208.sakenomi.discord.command.SettingsCommandHandler
import com.github.rain1208.sakenomi.discord.listener.InteractionListener
import com.github.rain1208.sakenomi.repository.AttendanceRepository
import com.github.rain1208.sakenomi.repository.EventRepository
import com.github.rain1208.sakenomi.repository.GuildSettingsRepository
import com.github.rain1208.sakenomi.repository.ParticipantRepository
import com.github.rain1208.sakenomi.repository.PartyRepository
import com.github.rain1208.sakenomi.repository.PaymentRepository
import com.github.rain1208.sakenomi.service.AttendanceService
import com.github.rain1208.sakenomi.service.AuditLogger
import com.github.rain1208.sakenomi.service.EventService
import com.github.rain1208.sakenomi.service.ParticipantService
import com.github.rain1208.sakenomi.service.PartyService
import com.github.rain1208.sakenomi.service.PaymentService
import com.github.rain1208.sakenomi.service.SettingsService
import org.koin.dsl.module

fun appModule(config: AppConfig) = module {
    single { config }

    // Repository
    single { GuildSettingsRepository() }
    single { EventRepository() }
    single { ParticipantRepository() }
    single { PartyRepository() }
    single { PaymentRepository() }
    single { AttendanceRepository() }

    // Discord Gateway
    single { DiscordGateway() }

    // Service
    single { AuditLogger(get()) }
    single { SettingsService(get()) }
    single { EventService(get(), get(), get(), get(), get(), get()) }
    single { ParticipantService(get(), get(), get(), get()) }
    single { PartyService(get()) }
    single { PaymentService(get(), get()) }
    single { AttendanceService(get(), get(), get()) }

    // Handler
    single { SettingsCommandHandler(get(), get()) }
    single { EventCommandHandler(get(), get(), get()) }
    single { PartyCommandHandler(get(), get(), get(), get()) }
    single { PaymentCommandHandler(get(), get(), get()) }
    single { AttendanceCommandHandler(get(), get(), get(), get()) }
    single { EventButtonHandler(get(), get(), get()) }

    // Listener
    single { InteractionListener(get(), get(), get(), get(), get(), get()) }
}
