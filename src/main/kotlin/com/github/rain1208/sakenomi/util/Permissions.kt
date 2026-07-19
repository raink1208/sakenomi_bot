package com.github.rain1208.sakenomi.util

import com.github.rain1208.sakenomi.domain.Event
import com.github.rain1208.sakenomi.domain.GuildSettings
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member

object Permissions {

    /**
     * 運営判定。admin-role 保持者に加え、初期設定前でも操作できるよう
     * サーバー管理者(ADMINISTRATOR)も運営として扱う。
     */
    fun isAdmin(member: Member, settings: GuildSettings): Boolean {
        if (member.hasPermission(Permission.ADMINISTRATOR)) return true
        val adminRoleId = settings.adminRoleId ?: return false
        return member.roles.any { it.idLong == adminRoleId }
    }

    /** 幹事(作成者)または運営。admin-role は常に幹事権限を包含する(§8) */
    fun isOrganizerOrAdmin(member: Member, settings: GuildSettings, event: Event): Boolean =
        event.organizerUserId == member.idLong || isAdmin(member, settings)
}
