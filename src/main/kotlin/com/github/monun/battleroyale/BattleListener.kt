package com.github.monun.battleroyale

import net.kyori.adventure.text.Component.text
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerQuitEvent

class BattleListener(
    private val process: BattleProcess
) : Listener {
    @EventHandler
    fun onPlayerLogin(event: PlayerLoginEvent) {
        val player = event.player
        val battle = process.player(player)
        if (battle == null) {
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, text("게임 참가자가 아닙니다."))
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        event.joinMessage(null)

        val p = event.player
        val player = process.player(p)

        if (player == null) {
            p.gameMode = GameMode.SPECTATOR
        } else {
            player.player = p

            if (player.rank >= 0) {
                p.gameMode = GameMode.SPECTATOR
            }
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        event.quitMessage(null)

        val p = event.player
        process.player(p)?.let {
            it.player = null
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        event.deathMessage(null)

        val player = event.entity
        process.player(player)?.let { victim ->
            if (victim.rank != -1) return@let

            player.killer?.let { killer ->
                process.player(killer)?.let {
                    it.kills++
                }
            }
            process.rank(victim)
        }
    }
}