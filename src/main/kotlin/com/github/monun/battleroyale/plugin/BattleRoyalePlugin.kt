package com.github.monun.battleroyale.plugin

import com.github.monun.battleroyale.BattleProcess
import com.github.monun.kommand.kommand
import com.github.monun.kommand.sendFeedback
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

/**
 * @author Monun
 */
class BattleRoyalePlugin : JavaPlugin() {
    private lateinit var processFile: File
    private var process: BattleProcess? = null

    override fun onEnable() {
        dataFolder.mkdirs()
        processFile = File(dataFolder, "process.yml")

        setupKommands()
    }

    override fun onDisable() {
        process?.save()
    }

    fun processStart() {
        require(process == null) { "Process is already running" }

        process = BattleProcess(this, processFile)
    }

    fun processLoad() {
        require(process == null) { "Process is already running" }
        require(processFile.exists()) { "Process file not exists" }

        process = BattleProcess(this, processFile, true)
    }

    fun processStop() {
        process?.let {
            it.unregister()
            process = null
            processFile.delete()
        }
    }

    private fun setupKommands() = kommand {
        register("battle") {
            then("start") {
                executes {
                    val sender = it.sender
                    kotlin.runCatching {
                        processStart()
                    }.onSuccess {
                        sender.sendFeedback("전투 시작!")
                    }.onFailure { exception ->
                        sender.sendFeedback("전투 시작 실패 ${exception.message}")
                        exception.printStackTrace()
                    }
                }
            }
            then("stop") {
                executes {
                    processStop()
                    it.sender.sendFeedback("전투 종료")
                }
            }
            then("load") {
                executes {
                    val sender = it.sender
                    kotlin.runCatching {
                        processLoad()
                    }.onSuccess {
                        sender.sendFeedback("전투 재시작!")
                    }.onFailure { exception ->
                        sender.sendFeedback("전투 재시작 실패 ${exception.message}")
                    }
                }
            }
        }
    }
}