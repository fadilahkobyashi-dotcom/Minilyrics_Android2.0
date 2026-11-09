package com.example.model

import androidx.compose.ui.graphics.Color

enum class LyricsDisplayMode(val label: String) {
    ORIGINAL("Original"),
    ROMAJI("Romaji"),
    DUAL("Dual Lyrics")
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class LyricsAlignment {
    LEFT,
    CENTER,
    RIGHT
}

enum class ThemeOption(
    val title: String,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val activeLyricColor: Color,
    val inactiveLyricColor: Color,
    val textColor: Color,
    val subtextColor: Color,
    val isDark: Boolean = true
) {
    CLASSIC_DARK(
        title = "Classic MiniLyrics",
        background = Color(0xFF141923),
        surface = Color(0xFF1D2433),
        surfaceVariant = Color(0xFF283247),
        primary = Color(0xFF00E5FF),
        activeLyricColor = Color(0xFFFFD54F),
        inactiveLyricColor = Color(0xFF8E9EB5),
        textColor = Color(0xFFFFFFFF),
        subtextColor = Color(0xFFA0B0C8),
        isDark = true
    ),
    LIGHT_MODE(
        title = "Light Mode",
        background = Color(0xFFF6F8FC),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE5ECF6),
        primary = Color(0xFF0288D1),
        activeLyricColor = Color(0xFF0277BD),
        inactiveLyricColor = Color(0xFF90A4AE),
        textColor = Color(0xFF1E293B),
        subtextColor = Color(0xFF64748B),
        isDark = false
    ),
    AMOLED_BLACK(
        title = "AMOLED Black",
        background = Color(0xFF000000),
        surface = Color(0xFF0E0E0E),
        surfaceVariant = Color(0xFF1A1A1A),
        primary = Color(0xFF00E676),
        activeLyricColor = Color(0xFF00FF7F),
        inactiveLyricColor = Color(0xFF616161),
        textColor = Color(0xFFFFFFFF),
        subtextColor = Color(0xFF9E9E9E),
        isDark = true
    ),
    OCEAN_BLUE(
        title = "Ocean Blue",
        background = Color(0xFF0A192F),
        surface = Color(0xFF112240),
        surfaceVariant = Color(0xFF1D3557),
        primary = Color(0xFF64FFDA),
        activeLyricColor = Color(0xFF38BDF8),
        inactiveLyricColor = Color(0xFF748CAB),
        textColor = Color(0xFFF1F5F9),
        subtextColor = Color(0xFF94A3B8),
        isDark = true
    ),
    PURPLE_NIGHT(
        title = "Purple Night",
        background = Color(0xFF120C1F),
        surface = Color(0xFF1E1433),
        surfaceVariant = Color(0xFF2E1F4D),
        primary = Color(0xFFC084FC),
        activeLyricColor = Color(0xFFF472B6),
        inactiveLyricColor = Color(0xFF8B7A9E),
        textColor = Color(0xFFFAF5FF),
        subtextColor = Color(0xFFA89FBA),
        isDark = true
    ),
    SAKURA_PINK(
        title = "Sakura Pink",
        background = Color(0xFFFFF0F5),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFFFDDE6),
        primary = Color(0xFFF43F5E),
        activeLyricColor = Color(0xFFE11D48),
        inactiveLyricColor = Color(0xFFFDA4AF),
        textColor = Color(0xFF4C0519),
        subtextColor = Color(0xFF9F1239),
        isDark = false
    ),
    FOREST_GREEN(
        title = "Forest Green",
        background = Color(0xFF091C15),
        surface = Color(0xFF112E23),
        surfaceVariant = Color(0xFF1C4535),
        primary = Color(0xFF34D399),
        activeLyricColor = Color(0xFFA7F3D0),
        inactiveLyricColor = Color(0xFF6E9182),
        textColor = Color(0xFFF0FDF4),
        subtextColor = Color(0xFFA7C4B5),
        isDark = true
    ),
    SUNSET_ORANGE(
        title = "Sunset Orange",
        background = Color(0xFF1A120B),
        surface = Color(0xFF2B1E14),
        surfaceVariant = Color(0xFF3E2C1E),
        primary = Color(0xFFFB923C),
        activeLyricColor = Color(0xFFFBBF24),
        inactiveLyricColor = Color(0xFFA8896F),
        textColor = Color(0xFFFFFBEB),
        subtextColor = Color(0xFFD4BFA7),
        isDark = true
    ),
    MINIMALIST(
        title = "Minimalist",
        background = Color(0xFF18181B),
        surface = Color(0xFF27272A),
        surfaceVariant = Color(0xFF3F3F46),
        primary = Color(0xFFFAFAFA),
        activeLyricColor = Color(0xFFFFFFFF),
        inactiveLyricColor = Color(0xFF71717A),
        textColor = Color(0xFFF4F4F5),
        subtextColor = Color(0xFFA1A1AA),
        isDark = true
    ),
    GRADIENT_AURORA(
        title = "Gradient Aurora",
        background = Color(0xFF0D1B2A),
        surface = Color(0xFF1B263B),
        surfaceVariant = Color(0xFF2B3A52),
        primary = Color(0xFFEE9CA7),
        activeLyricColor = Color(0xFFFF758C),
        inactiveLyricColor = Color(0xFF8896A8),
        textColor = Color(0xFFFFFFFF),
        subtextColor = Color(0xFFB0BDCF),
        isDark = true
    ),
    NEON_CYBERPUNK(
        title = "Neon Cyberpunk",
        background = Color(0xFF080810),
        surface = Color(0xFF131326),
        surfaceVariant = Color(0xFF212140),
        primary = Color(0xFF00F0FF),
        activeLyricColor = Color(0xFFFF0055),
        inactiveLyricColor = Color(0xFF6B6B99),
        textColor = Color(0xFFF0F0FF),
        subtextColor = Color(0xFFA0A0D0),
        isDark = true
    ),
    GLASSMORPHISM(
        title = "Glassmorphism",
        background = Color(0xFF1E2330),
        surface = Color(0x662C3447),
        surfaceVariant = Color(0x883E4963),
        primary = Color(0xFF38BDF8),
        activeLyricColor = Color(0xFF7DD3FC),
        inactiveLyricColor = Color(0xFF94A3B8),
        textColor = Color(0xFFFFFFFF),
        subtextColor = Color(0xFFCBD5E1),
        isDark = true
    )
}
