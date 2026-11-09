package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ThemeOption
import com.example.ui.MainTab

@Composable
fun MiniLyricsHeader(
    currentTab: MainTab,
    theme: ThemeOption,
    onTabSelected: (MainTab) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(theme.surface)
            .statusBarsPadding()
    ) {
        // App Title Row with Action Icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "MiniLyrics",
                    tint = theme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "MiniLyrics",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textColor
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onOpenEqualizer,
                    modifier = Modifier.testTag("button_equalizer")
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Equalizer",
                        tint = theme.primary
                    )
                }

                IconButton(
                    onClick = onOpenThemes,
                    modifier = Modifier.testTag("button_themes")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Themes",
                        tint = theme.textColor
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("button_settings")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = theme.textColor
                    )
                }
            }
        }

        // Classic Tabs Row (Playback, Songs, Artists, Albums, Playlists)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MainTab.values().forEach { tab ->
                val isSelected = tab == currentTab
                val tabBg = if (isSelected) theme.primary.copy(alpha = 0.2f) else Color.Transparent
                val tabTextColor = if (isSelected) theme.primary else theme.subtextColor

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(tabBg)
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("tab_${tab.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val icon = when (tab) {
                            MainTab.PLAYBACK -> Icons.Default.PlayCircle
                            MainTab.SONGS -> Icons.Default.LibraryMusic
                            MainTab.ARTISTS -> Icons.Default.Person
                            MainTab.ALBUMS -> Icons.Default.Album
                            MainTab.PLAYLISTS -> Icons.Default.QueueMusic
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = tab.title,
                            tint = tabTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = tab.title,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = tabTextColor
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = theme.surfaceVariant, thickness = 1.dp)
    }
}
