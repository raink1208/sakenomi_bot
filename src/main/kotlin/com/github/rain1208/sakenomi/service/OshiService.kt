package com.github.rain1208.sakenomi.service

import com.github.rain1208.sakenomi.discord.DiscordGateway
import com.github.rain1208.sakenomi.domain.Oshi
import com.github.rain1208.sakenomi.domain.ServiceException
import com.github.rain1208.sakenomi.repository.OshiRepository
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.exceptions.HierarchyException
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException

/** 推しロールの登録管理とメンバーへの付与・剥奪 */
class OshiService(
    private val oshiRepository: OshiRepository,
    private val gateway: DiscordGateway,
) {

    data class ChangeResult(val added: List<Oshi>, val removed: List<Oshi>)

    fun list(guildId: Long): List<Oshi> = oshiRepository.listByGuild(guildId)

    /**
     * Discord 上にロールが残っている推しのみ(手動でロールが削除された推しは選択肢に出さない)。
     * 並び順は Discord のロール一覧と同じ(上位のロールが先頭)。運営はサーバー設定でロールを
     * 並べ替えるだけで選択肢の順番を変えられる
     */
    fun listAvailable(guild: Guild): List<Pair<Oshi, Role>> =
        list(guild.idLong)
            .mapNotNull { oshi -> guild.getRoleById(oshi.roleId)?.let { oshi to it } }
            // Role の自然順序は position 昇順(同順位は ID で決定)。Discord の表示は上位が先頭のため降順にする
            .sortedByDescending { (_, role) -> role }

    fun register(guild: Guild, role: Role, name: String): Oshi {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) throw ServiceException("推しの名前を入力してください")
        if (role.isPublicRole) throw ServiceException("@everyone は推しロールに指定できません")
        if (role.isManaged) throw ServiceException("Bot・連携サービスが管理するロールは推しロールに指定できません")
        if (!guild.selfMember.canInteract(role)) {
            throw ServiceException(
                "${role.asMention} は Bot のロールより上位にあるため付与できません。" +
                    "サーバー設定でロールの並び順を Bot のロールより下に移動してください",
            )
        }
        oshiRepository.findByRoleId(guild.idLong, role.idLong)?.let {
            throw ServiceException("${role.asMention} は既に「${it.name}」として登録されています")
        }
        if (oshiRepository.findByName(guild.idLong, trimmed) != null) {
            throw ServiceException("「$trimmed」は既に登録されています")
        }
        if (oshiRepository.count(guild.idLong) >= MAX_OSHI) {
            throw ServiceException("推しは最大 ${MAX_OSHI} 件まで登録できます")
        }
        return oshiRepository.insert(guild.idLong, role.idLong, trimmed)
    }

    /** 紐付けのみ解除する。Discord のロール自体・メンバーへの付与状態は変更しない */
    fun unregister(guildId: Long, name: String): Oshi {
        val oshi = oshiRepository.findByName(guildId, name.trim())
            ?: throw ServiceException("「${name.trim()}」は登録されていません")
        oshiRepository.delete(oshi.id)
        return oshi
    }

    /**
     * パネルで選択された推しをメンバーに反映する。
     * [offeredRoleIds] はパネルに表示した選択肢、[selectedRoleIds] はそのうち選択されたもの。
     * 選択肢に出ていたが選択されなかった推しロールは剥奪する(パネル表示後に追加された推しは触らない)
     */
    fun applySelection(
        guild: Guild,
        member: Member,
        offeredRoleIds: Set<Long>,
        selectedRoleIds: Set<Long>,
    ): ChangeResult {
        val registered = list(guild.idLong).associateBy { it.roleId }
        val offered = offeredRoleIds.filter { it in registered }.toSet()
        val selected = selectedRoleIds.filter { it in offered }.toSet()
        val current = member.roles.map { it.idLong }.toSet()

        val toAdd = (selected - current).mapNotNull { registered[it] }
        val toRemove = ((offered - selected) intersect current).mapNotNull { registered[it] }
        if (toAdd.isEmpty() && toRemove.isEmpty()) return ChangeResult(emptyList(), emptyList())

        try {
            gateway.modifyRoles(guild, member, toAdd.map { it.roleId }, toRemove.map { it.roleId })
        } catch (e: HierarchyException) {
            throw ServiceException("Bot より上位のロールが含まれているため変更できませんでした。運営に連絡してください", e)
        } catch (e: InsufficientPermissionException) {
            throw ServiceException("Bot にロールの管理権限がないため変更できませんでした。運営に連絡してください", e)
        }
        return ChangeResult(toAdd, toRemove)
    }

    companion object {
        /** セレクトメニュー 1 つあたりの選択肢上限 */
        const val OPTIONS_PER_MENU = 25

        /** 1 メッセージに置けるセレクトメニューは 4 つ(残り 1 行は決定ボタン) */
        const val MAX_MENUS = 4

        const val MAX_OSHI = OPTIONS_PER_MENU * MAX_MENUS
    }
}
