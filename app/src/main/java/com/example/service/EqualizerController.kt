package com.example.service

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.util.Log

data class EqualizerBandInfo(
    val index: Int,
    val centerFreqHz: Int,
    val minLevelmB: Int,
    val maxLevelmB: Int,
    var currentLevelmB: Int
)

class EqualizerController {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var currentSessionId: Int = 0

    val presetNames = listOf(
        "Normal", "Bass Boost", "Treble Boost", "Vocal Boost",
        "Rock", "Pop", "Jazz", "Classical", "Dance", "Custom"
    )

    fun attachToSession(sessionId: Int, enabled: Boolean, bassLevel: Int) {
        if (sessionId <= 0 || sessionId == currentSessionId) return
        release()
        currentSessionId = sessionId

        try {
            equalizer = Equalizer(0, sessionId).apply {
                this.enabled = enabled
            }
        } catch (e: Exception) {
            Log.e("EqualizerController", "Error initializing Equalizer: ${e.message}")
        }

        try {
            bassBoost = BassBoost(0, sessionId).apply {
                if (strengthSupported) {
                    setStrength(bassLevel.toShort())
                    this.enabled = enabled && bassLevel > 0
                }
            }
        } catch (e: Exception) {
            Log.e("EqualizerController", "Error initializing BassBoost: ${e.message}")
        }
    }

    fun setEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
        } catch (e: Exception) {
            Log.e("EqualizerController", "Failed to toggle equalizer: ${e.message}")
        }
    }

    fun isEnabled(): Boolean = equalizer?.enabled ?: false

    fun getBands(): List<EqualizerBandInfo> {
        val eq = equalizer ?: return emptyList()
        val numBands = eq.numberOfBands.toInt()
        val range = eq.bandLevelRange ?: shortArrayOf(-1500, 1500)
        val list = mutableListOf<EqualizerBandInfo>()

        for (i in 0 until numBands) {
            val freqHz = eq.getCenterFreq(i.toShort()) / 1000
            val currentLevel = eq.getBandLevel(i.toShort()).toInt()
            list.add(
                EqualizerBandInfo(
                    index = i,
                    centerFreqHz = freqHz,
                    minLevelmB = range[0].toInt(),
                    maxLevelmB = range[1].toInt(),
                    currentLevelmB = currentLevel
                )
            )
        }
        return list
    }

    fun setBandLevel(bandIndex: Int, levelmB: Int) {
        try {
            equalizer?.setBandLevel(bandIndex.toShort(), levelmB.toShort())
        } catch (e: Exception) {
            Log.e("EqualizerController", "Failed to set band level: ${e.message}")
        }
    }

    fun setBassBoost(strength: Int) {
        try {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(strength.coerceIn(0, 1000).toShort())
                bassBoost?.enabled = strength > 0
            }
        } catch (e: Exception) {
            Log.e("EqualizerController", "Failed to set bass boost: ${e.message}")
        }
    }

    fun applyPreset(presetIndex: Int) {
        val eq = equalizer ?: return
        val bandsCount = eq.numberOfBands.toInt()

        val levels: List<Int> = when (presetIndex) {
            0 -> listOf(0, 0, 0, 0, 0) // Normal
            1 -> listOf(600, 400, 0, 0, 0) // Bass Boost
            2 -> listOf(0, 0, 200, 500, 700) // Treble Boost
            3 -> listOf(-200, 200, 600, 300, -100) // Vocal Boost
            4 -> listOf(500, 300, -100, 400, 600) // Rock
            5 -> listOf(-100, 200, 500, 100, -200) // Pop
            6 -> listOf(300, 100, -200, 200, 400) // Jazz
            7 -> listOf(400, 200, -100, 200, 400) // Classical
            8 -> listOf(600, 200, 300, 500, 200) // Dance
            else -> return // Custom (manual sliders)
        }

        for (i in 0 until minOf(bandsCount, levels.size)) {
            setBandLevel(i, levels[i])
        }
    }

    fun release() {
        try {
            equalizer?.release()
        } catch (ignored: Exception) {}
        try {
            bassBoost?.release()
        } catch (ignored: Exception) {}
        equalizer = null
        bassBoost = null
        currentSessionId = 0
    }
}
