package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.model.ThemeOption

@Composable
fun MiniLyricsTheme(
    themeOption: ThemeOption = ThemeOption.CLASSIC_DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = if (themeOption.isDark) {
        darkColorScheme(
            primary = themeOption.primary,
            background = themeOption.background,
            surface = themeOption.surface,
            surfaceVariant = themeOption.surfaceVariant,
            onBackground = themeOption.textColor,
            onSurface = themeOption.textColor,
            onPrimary = themeOption.background
        )
    } else {
        lightColorScheme(
            primary = themeOption.primary,
            background = themeOption.background,
            surface = themeOption.surface,
            surfaceVariant = themeOption.surfaceVariant,
            onBackground = themeOption.textColor,
            onSurface = themeOption.textColor,
            onPrimary = themeOption.textColor
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
