package com.github.rain1208.sakenomi.repository

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

// created_at / joined_at 等 DB デフォルトに任せる列は Exposed には定義しない

object GuildSettingsTable : Table("guild_settings") {
    val guildId = long("guild_id")
    val eventCategoryId = long("event_category_id").nullable()
    val archiveCategoryId = long("archive_category_id").nullable()
    val adminRoleId = long("admin_role_id").nullable()
    val logChannelId = long("log_channel_id").nullable()
    val recruitChannelId = long("recruit_channel_id").nullable()
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(guildId)
}

object EventCountersTable : Table("event_counters") {
    val guildId = long("guild_id")
    val nextNumber = integer("next_number")

    override val primaryKey = PrimaryKey(guildId)
}

object EventsTable : Table("events") {
    val id = long("id").autoIncrement()
    val guildId = long("guild_id")
    val number = integer("number")
    val name = varchar("name", 80)
    val area = varchar("area", 80)
    val eventDate = date("event_date").nullable()
    val status = varchar("status", 16)
    val channelId = long("channel_id")
    val roleId = long("role_id")
    val recruitMessageId = long("recruit_message_id").nullable()
    val organizerUserId = long("organizer_user_id")
    val archivedAt = timestampWithTimeZone("archived_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

object EventParticipantsTable : Table("event_participants") {
    val id = long("id").autoIncrement()
    val eventId = long("event_id")
    val userId = long("user_id")

    override val primaryKey = PrimaryKey(id)
}

object PartiesTable : Table("parties") {
    val id = long("id").autoIncrement()
    val eventId = long("event_id")
    val orderNo = integer("order_no")
    val shopName = varchar("shop_name", 120)
    val totalAmount = long("total_amount").nullable()
    val note = varchar("note", 255).nullable()

    override val primaryKey = PrimaryKey(id)
}

object PaymentsTable : Table("payments") {
    val id = long("id").autoIncrement()
    val partyId = long("party_id")
    val payerUserId = long("payer_user_id")
    val amount = long("amount")
    val recordedBy = long("recorded_by")
    val receivedAt = timestampWithTimeZone("received_at")

    override val primaryKey = PrimaryKey(id)
}

object AttendancesTable : Table("attendances") {
    val id = long("id").autoIncrement()
    val partyId = long("party_id")
    val userId = long("user_id")
    val status = varchar("status", 16)
    val recordedBy = long("recorded_by")
    val recordedAt = timestampWithTimeZone("recorded_at")

    override val primaryKey = PrimaryKey(id)
}