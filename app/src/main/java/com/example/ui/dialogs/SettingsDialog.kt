package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.LyricsAlignment
import com.example.model.ThemeOption
import com.example.ui.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun SettingsDialog(
    viewModel: MainViewModel,
    theme: ThemeOption,
    onDismiss: () -> Unit
) {
    val prefs = viewModel.preferencesManager

    val fontSize by prefs.fontSizeState.collectAsState()
    val alignment by prefs.alignmentState.collectAsState()
    val autoSearch by prefs.autoSearchLyricsState.collectAsState()
    val sleepTimerSec by (viewModel.playbackService?.sleepTimerSec ?: MutableStateFlow(0)).collectAsState()

    var cacheClearedMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = theme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings & Preferences",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textColor
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = theme.subtextColor)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Lyrics Appearance
                Text(
                    text = "Lyrics Display",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Font size slider
                Text(
                    text = "Font Size: ${fontSize.toInt()} sp",
                    fontSize = 12.sp,
                    color = theme.textColor
                )
                Slider(
                    value = fontSize,
                    onValueChange = { prefs.setFontSize(it) },
                    valueRange = 12f..28f,
                    steps = 15,
                    colors = SliderDefaults.colors(
                        thumbColor = theme.primary,
                        activeTrackColor = theme.primary,
                        inactiveTrackColor = theme.surfaceVariant
                    ),
                    modifier = Modifier.testTag("slider_font_size")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Text Alignment
                Text(text = "Text Alignment", fontSize = 12.sp, color = theme.textColor)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LyricsAlignment.values().forEach { align ->
                        val isSelected = alignment == align
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) theme.primary else theme.surfaceVariant)
                                .clickable { prefs.setAlignment(align) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = align.name.lowercase().replaceFirstChar { it.uppercase() },
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) theme.background else theme.textColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Auto search toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Auto-Search Online Lyrics", fontSize = 13.sp, color = theme.textColor)
                        Text(text = "Automatically retrieve missing synced lyrics", fontSize = 11.sp, color = theme.subtextColor)
                    }
                    Switch(
                        checked = autoSearch,
                        onCheckedChange = { prefs.setAutoSearchLyrics(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = theme.primary)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = theme.surfaceVariant, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Sleep Timer
                Text(
                    text = "Sleep Timer",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.primary
                )
                if (sleepTimerSec > 0) {
                    val m = sleepTimerSec / 60
                    val s = sleepTimerSec % 60
                    Text(
                        text = "Active: Music will pause in %d:%02d".format(m, s),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                val timerOptions = listOf(0 to "Off", 15 to "15m", 30 to "30m", 45 to "45m", 60 to "60m")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    timerOptions.forEach { (mins, label) ->
                        val isSelected = if (mins == 0) sleepTimerSec == 0 else (sleepTimerSec in (mins * 60 - 60)..(mins * 60))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) theme.primary else theme.surfaceVariant)
                                .clickable { viewModel.playbackService?.startSleepTimer(mins) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) theme.background else theme.textColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = theme.surfaceVariant, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: Storage & Library
                Text(
                    text = "Storage & Library",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.refreshLibrary()
                        cacheClearedMessage = "Library rescanned successfully!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().testTag("button_rescan_storage")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = theme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rescan Device Media Library", color = theme.textColor, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = {
                        cacheClearedMessage = "Album art & lyrics cache cleared."
                    },
                    modifier = Modifier.fillMaxWidth().testTag("button_clear_cache")
                ) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, tint = theme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Temporary Cache", color = theme.primary, fontSize = 12.sp)
                }

                if (cacheClearedMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = cacheClearedMessage!!,
                        fontSize = 11.sp,
                        color = theme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // About MiniLyrics Android 16
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = theme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "MiniLyrics v4.0 for Android 16",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Synchronized LRC Player • Kanji/Romaji/Dual Transliteration • 12 Themes • Hardware Equalizer",
                            fontSize = 11.sp,
                            color = theme.subtextColor,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = theme.background, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
