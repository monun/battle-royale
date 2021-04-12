package com.github.monun.battleroyale

import com.github.monun.battleroyale.plugin.BattleRoyalePlugin
import com.google.common.collect.ImmutableMap
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.TextColor.color
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.scheduler.BukkitTask
import java.io.File
import java.time.Duration
import java.util.*

class BattleProcess(
    private val plugin: BattleRoyalePlugin,
    private val processFile: File,
    load: Boolean = false
) {
    private val players: Map<UUID, BattlePlayer>
    private val onlinePlayers
        get() = Bukkit.getOnlinePlayers().mapNotNull { players[it.uniqueId] }
    private val survivePlayers
        get() = players.values.filter { it.rank == -1 }
    private val knockoutPlayers
        get() = players.values.filter { it.rank >= 0}

    private var currentRank: Int

    private val battleListener: BattleListener
    private val bukkitTask: BukkitTask

    init {
        players = ImmutableMap.copyOf(
            if (!load) {
                Bukkit.getOnlinePlayers().asSequence().filter {
                    it.gameMode.isDamageable
                }.associate { p ->
                    p.uniqueId to BattlePlayer(this, p.uniqueId, p.name).apply {
                        player = p
                    }
                }
            } else {
                val yaml = YamlConfiguration.loadConfiguration(processFile)
                val players = HashMap<UUID, BattlePlayer>()

                for ((name, value) in yaml.getValues(false).filter { it.value is ConfigurationSection }) {
                    val section = value as ConfigurationSection
                    val battlePlayer = BattlePlayer(this, UUID.fromString(name), section.getString("name")!!).apply {
                        player = Bukkit.getPlayer(uniqueId)
                        rank = section.getInt("rank")
                        kills = section.getInt("kills")
                    }
                    players[battlePlayer.uniqueId] = battlePlayer
                }
                players
            }
        )
        currentRank = survivePlayers.count()

        Bukkit.getOnlinePlayers().forEach { 
            if (!it.isOp && it.uniqueId !in players) {
                it.kick(text("게임 참가자가 아닙니다."))
            }
        }

        plugin.server.apply {
            battleListener = BattleListener(this@BattleProcess).also {
                pluginManager.registerEvents(it, plugin)
            }
            bukkitTask = scheduler.runTaskTimer(plugin, this@BattleProcess::onUpdate, 0L, 1L)
        }
    }

    fun unregister() {
        HandlerList.unregisterAll(battleListener)
        bukkitTask.cancel()
    }

    fun player(uuid: UUID) = players[uuid]

    fun player(player: Player) = player(player.uniqueId)

    private fun onUpdate() {
        survivePlayers.forEach { it.onUpdate() }
    }

    fun rank(player: BattlePlayer) {
        require(player.rank == -1) { "Cannot redefine rank ${player.name}" }

        player.rank = currentRank--

        save()

        player.player?.let { it.gameMode = GameMode.SPECTATOR }

        val offlinePlayer = player.offlinePlayer
        if (!offlinePlayer.isOp) {
            player.offlinePlayer.banPlayer("#${player.rank} ${player.name} - ${player.kills} kills", "BATTLEROYALE")
        }

        plugin.server.sendMessage(text("#${player.rank} ${player.name} - ${player.kills} kills").decorate(TextDecoration.BOLD))

        val survivors = survivePlayers

        if (survivors.count() <= 1) {
            Bukkit.setWhitelist(true)
            var message: Component = text("")

            survivors.firstOrNull()?.let { champion ->
                champion.offlinePlayer.isWhitelisted = true
                message =
                    text("#1 ${champion.name} - ${champion.kills} kills").decorate(TextDecoration.BOLD)
                        .color(color(0xFFD700))
            }

            plugin.server.run {
                sendMessage(message)
                showTitle(
                    Title.title(
                        text("게임종료!").color(color(0xFF0000)).decorate(TextDecoration.BOLD),
                        message,
                        Title.Times.of(
                            Duration.ofSeconds(1),
                            Duration.ofSeconds(10),
                            Duration.ofSeconds(1)
                        )
                    )
                )
            }

            plugin.processStop()
        }
    }

    fun save() {
        val yaml = YamlConfiguration()
        for ((uuid, player) in players) {
            yaml.createSection(uuid.toString()).let { section ->
                section["name"] = player.name
                section["rank"] = player.rank
                section["kills"] = player.kills
            }
        }

        yaml.save(processFile.also { it.parentFile.mkdirs() })
    }
}

private val GameMode.isDamageable
    get() = this == GameMode.SURVIVAL || this == GameMode.ADVENTURE