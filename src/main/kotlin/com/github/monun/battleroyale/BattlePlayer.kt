package com.github.monun.battleroyale

import com.github.monun.tap.ref.weaky
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.*

class BattlePlayer(
    private val process: BattleProcess,
    val uniqueId: UUID,
    name: String
) {
    var name: String = name
        get() {
            player?.let { field = it.name }
            return field
        }

    var player: Player? by weaky(null) { Bukkit.getPlayer(uniqueId) }

    val offlinePlayer: OfflinePlayer
        get() {
            return player ?: Bukkit.getOfflinePlayer(uniqueId)
        }

    var knockoutTicks = 0L

    var kills = 0

    var rank = -1

    val isOnline
        get() = player != null

    // 살아남았을 경우에만 호출
    fun onUpdate() {
        val player = player

        if (player == null) {
            knockoutTicks++

            if (knockoutTicks > 600) {
                process.rank(this)
            }
            println("$name $knockoutTicks")
        } else {
            knockoutTicks = 0
        }
    }
}
