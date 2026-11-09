package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.LyricsAlignment
import com.example.model.LyricsDisplayMode
import com.example.model.ThemeOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("minilyrics_prefs", Context.MODE_PRIVATE)

    private val _themeState = MutableStateFlow(getSavedTheme())
    val themeState: StateFlow<ThemeOption> = _themeState.asStateFlow()

    private val _displayModeState = MutableStateFlow(getSavedDisplayMode())
    val displayModeState: StateFlow<LyricsDisplayMode> = _displayModeState.asStateFlow()

    private val _fontSizeState = MutableStateFlow(prefs.getFloat("lyrics_font_size", 18f))
    val fontSizeState: StateFlow<Float> = _fontSizeState.asStateFlow()

    private val _alignmentState = MutableStateFlow(getSavedAlignment())
    val alignmentState: StateFlow<LyricsAlignment> = _alignmentState.asStateFlow()

    private val _autoSearchLyricsState = MutableStateFlow(prefs.getBoolean("auto_search_lyrics", true))
    val autoSearchLyricsState: StateFlow<Boolean> = _autoSearchLyricsState.asStateFlow()

    fun setTheme(theme: ThemeOption) {
        prefs.edit().putString("app_theme", theme.name).apply()
        _themeState.value = theme
    }

    private fun getSavedTheme(): ThemeOption {
        val name = prefs.getString("app_theme", ThemeOption.CLASSIC_DARK.name)
        return try {
            ThemeOption.valueOf(name ?: ThemeOption.CLASSIC_DARK.name)
        } catch (e: Exception) {
            ThemeOption.CLASSIC_DARK
        }
    }

    fun setDisplayMode(mode: LyricsDisplayMode) {
        prefs.edit().putString("lyrics_display_mode", mode.name).apply()
        _displayModeState.value = mode
    }

    private fun getSavedDisplayMode(): LyricsDisplayMode {
        val name = prefs.getString("lyrics_display_mode", LyricsDisplayMode.ORIGINAL.name)
        return try {
            LyricsDisplayMode.valueOf(name ?: LyricsDisplayMode.ORIGINAL.name)
        } catch (e: Exception) {
            LyricsDisplayMode.ORIGINAL
        }
    }

    fun setFontSize(size: Float) {
        prefs.edit().putFloat("lyrics_font_size", size).apply()
        _fontSizeState.value = size
    }

    fun setAlignment(align: LyricsAlignment) {
        prefs.edit().putString("lyrics_alignment", align.name).apply()
        _alignmentState.value = align
    }

    private fun getSavedAlignment(): LyricsAlignment {
        val name = prefs.getString("lyrics_alignment", LyricsAlignment.CENTER.name)
        return try {
            LyricsAlignment.valueOf(name ?: LyricsAlignment.CENTER.name)
        } catch (e: Exception) {
            LyricsAlignment.CENTER
        }
    }

    fun setAutoSearchLyrics(enabled: Boolean) {
        prefs.edit().putBoolean("auto_search_lyrics", enabled).apply()
        _autoSearchLyricsState.value = enabled
    }

    // Equalizer
    fun isEqEnabled(): Boolean = prefs.getBoolean("eq_enabled", true)
    fun setEqEnabled(enabled: Boolean) = prefs.edit().putBoolean("eq_enabled", enabled).apply()

    fun getEqPreset(): Int = prefs.getInt("eq_preset", 0)
    fun setEqPreset(preset: Int) = prefs.edit().putInt("eq_preset", preset).apply()

    fun getBassBoost(): Int = prefs.getInt("bass_boost", 0)
    fun setBassBoost(level: Int) = prefs.edit().putInt("bass_boost", level).apply()

    fun getBandLevel(band: Int): Int = prefs.getInt("eq_band_$band", 0)
    fun setBandLevel(band: Int, level: Int) = prefs.edit().putInt("eq_band_$band", level).apply()
}
