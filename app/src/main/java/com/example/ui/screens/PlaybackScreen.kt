package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.LyricsDisplayMode
import com.example.model.LyricsAlignment
import com.example.model.RepeatMode as SongRepeatMode
import com.example.model.Song
import com.example.model.ThemeOption
import com.example.ui.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun PlaybackScreen(
    viewModel: MainViewModel,
    theme: ThemeOption,
    modifier: Modifier = Modifier
) {
    val service = viewModel.playbackService
    val currentSong by (service?.currentSong ?: MutableStateFlow(null)).collectAsState()
    val isPlaying by (service?.isPlaying ?: MutableStateFlow(false)).collectAsState()
    val currentPosMs by (service?.currentPosition ?: MutableStateFlow(0L)).collectAsState()
    val durationMs by (service?.duration ?: MutableStateFlow(0L)).collectAsState()
    val repeatMode by (service?.repeatMode ?: MutableStateFlow(SongRepeatMode.ALL)).collectAsState()
    val isShuffle by (service?.isShuffle ?: MutableStateFlow(false)).collectAsState()

    val lyricsData by viewModel.currentLyrics.collectAsState()
    val isLoadingLyrics by viewModel.isLoadingLyrics.collectAsState()

    val displayMode by viewModel.preferencesManager.displayModeState.collectAsState()
    val fontSize by viewModel.preferencesManager.fontSizeState.collectAsState()
    val alignment by viewModel.preferencesManager.alignmentState.collectAsState()

    val favoriteIds by viewModel.favoriteSongIds.collectAsState()
    val isFavorite = currentSong?.let { favoriteIds.contains(it.id) } ?: false

    // Vinyl rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Calculate active lyric index
    val activeLineIndex = remember(lyricsData.lines, currentPosMs) {
        val lines = lyricsData.lines
        if (lines.isEmpty()) -1
        else {
            val idx = lines.indexOfLast { it.timeMs <= currentPosMs }
            if (idx >= 0) idx else 0
        }
    }

    val listState = rememberLazyListState()

    // Smooth scroll to active line
    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex >= 0 && activeLineIndex < lyricsData.lines.size) {
            listState.animateScrollToItem(
                index = maxOf(0, activeLineIndex - 2),
                scrollOffset = 0
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Track Title & Artist Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentSong?.title ?: "No track selected",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentSong?.artist ?: "Select a song to play",
                    fontSize = 13.sp,
                    color = theme.subtextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Lyrics Source badge / Edit shortcut
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AssistChip(
                    onClick = { viewModel.openSearchLyricsDialog(currentSong) },
                    label = {
                        Text(
                            text = if (lyricsData.lines.isNotEmpty()) lyricsData.source else "Find Lyrics",
                            fontSize = 11.sp,
                            color = theme.primary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = theme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(containerColor = theme.surfaceVariant),
                    border = null,
                    modifier = Modifier.testTag("button_search_lyrics_badge")
                )

                if (currentSong != null) {
                    IconButton(
                        onClick = { viewModel.openEditTrackDialog(currentSong!!) },
                        modifier = Modifier.size(32.dp).testTag("button_edit_track")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Track Info",
                            tint = theme.subtextColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Center Area: Symmetrical Centered Canvas with Enlarged Translucent Album Art Watermark & Synchronized Lyrics
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(theme.surface)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Symmetrical, Enlarged, Translucent Center Backdrop Artwork/Vinyl (Rotating when playing)
            val embeddedArt = currentSong?.embeddedArt
            val bitmap = remember(embeddedArt) {
                if (embeddedArt != null && embeddedArt.isNotEmpty()) {
                    BitmapFactory.decodeByteArray(embeddedArt, 0, embeddedArt.size)
                } else null
            }

            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .rotate(if (isPlaying) rotation else 0f)
                    .alpha(0.18f),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Album Art Watermark",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.default_album_art),
                        contentDescription = "Album Art Watermark",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                // Vinyl Grooves effect overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(4.dp, theme.primary.copy(alpha = 0.4f), CircleShape)
                        .border(16.dp, Color.Black.copy(alpha = 0.25f), CircleShape)
                )

                // Center Spindle Hole
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(theme.background)
                        .border(2.dp, theme.primary.copy(alpha = 0.6f), CircleShape)
                )
            }

            // Foreground: Lyrics Mode switcher & Synchronized Scrolling Lyrics
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Centered Album Artwork Mini Pill + Lyrics Mode Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Symmetrical Mini Album Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.surfaceVariant.copy(alpha = 0.7f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .rotate(if (isPlaying) rotation else 0f)
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.default_album_art),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Text(
                            text = currentSong?.album ?: "MiniLyrics",
                            fontSize = 11.sp,
                            color = theme.subtextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 120.dp)
                        )
                    }

                    // Lyrics View Mode Switcher [Original] [Romaji] [Dual]
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    LyricsDisplayMode.values().forEach { mode ->
                        val selected = displayMode == mode
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) theme.primary else theme.surfaceVariant)
                                .clickable { viewModel.preferencesManager.setDisplayMode(mode) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("lyrics_mode_${mode.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) theme.background else theme.textColor
                            )
                        }
                    }
                }
            }

                // Lyrics Content List
                if (isLoadingLyrics) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = theme.primary, modifier = Modifier.size(28.dp))
                    }
                } else if (lyricsData.lines.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No synchronized lyrics found",
                                fontSize = 13.sp,
                                color = theme.subtextColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.openSearchLyricsDialog(currentSong) },
                                colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                                modifier = Modifier.testTag("button_search_online_fallback")
                            ) {
                                Text("Search Online", color = theme.background, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    val textAlign = when (alignment) {
                        LyricsAlignment.LEFT -> TextAlign.Start
                        LyricsAlignment.CENTER -> TextAlign.Center
                        LyricsAlignment.RIGHT -> TextAlign.End
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 40.dp)
                    ) {
                        itemsIndexed(lyricsData.lines) { index, line ->
                            val isActive = index == activeLineIndex
                            val lineColor = if (isActive) theme.activeLyricColor else theme.inactiveLyricColor
                            val currentFontSize = if (isActive) (fontSize + 2).sp else fontSize.sp

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isActive) theme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable {
                                        // Tap-to-seek functionality!
                                        if (line.timeMs > 0) {
                                            viewModel.seekTo(line.timeMs)
                                        }
                                    }
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                horizontalAlignment = when (alignment) {
                                    LyricsAlignment.LEFT -> Alignment.Start
                                    LyricsAlignment.CENTER -> Alignment.CenterHorizontally
                                    LyricsAlignment.RIGHT -> Alignment.End
                                }
                            ) {
                                when (displayMode) {
                                    LyricsDisplayMode.ORIGINAL -> {
                                        Text(
                                            text = line.text,
                                            fontSize = currentFontSize,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                            color = lineColor,
                                            textAlign = textAlign,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    LyricsDisplayMode.ROMAJI -> {
                                        Text(
                                            text = if (line.romaji.isNotBlank()) line.romaji else line.text,
                                            fontSize = currentFontSize,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                            color = lineColor,
                                            textAlign = textAlign,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    LyricsDisplayMode.DUAL -> {
                                        // Original on top
                                        Text(
                                            text = line.text,
                                            fontSize = currentFontSize,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                            color = lineColor,
                                            textAlign = textAlign,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        // Romaji underneath
                                        if (line.romaji.isNotBlank() && line.romaji != line.text) {
                                            Text(
                                                text = line.romaji,
                                                fontSize = (fontSize - 3).sp,
                                                color = if (isActive) lineColor.copy(alpha = 0.85f) else lineColor.copy(alpha = 0.6f),
                                                textAlign = textAlign,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Timing Calibration Bar: [< -0.5s] [Offset: 0.0s] [> +0.5s]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { viewModel.adjustLyricsOffset(-500L) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp).testTag("button_offset_minus")
            ) {
                Text("-0.5s", fontSize = 11.sp, color = theme.primary)
            }

            TextButton(
                onClick = { viewModel.resetLyricsOffset() },
                modifier = Modifier.height(30.dp).testTag("button_offset_reset")
            ) {
                val offsetSec = lyricsData.offsetMs / 1000f
                Text(
                    text = "Offset: ${if (offsetSec >= 0) "+%.1f".format(offsetSec) else "%.1f".format(offsetSec)}s",
                    fontSize = 11.sp,
                    color = theme.textColor
                )
            }

            OutlinedButton(
                onClick = { viewModel.adjustLyricsOffset(500L) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp).testTag("button_offset_plus")
            ) {
                Text("+0.5s", fontSize = 11.sp, color = theme.primary)
            }
        }

        // Progress Scrub Bar
        val progress = if (durationMs > 0) currentPosMs.toFloat() / durationMs.toFloat() else 0f
        var isScrubbing by remember { mutableStateOf(false) }
        var scrubValue by remember { mutableStateOf(0f) }

        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = if (isScrubbing) scrubValue else progress.coerceIn(0f, 1f),
                onValueChange = {
                    isScrubbing = true
                    scrubValue = it
                },
                onValueChangeFinished = {
                    isScrubbing = false
                    val targetMs = (scrubValue * durationMs).toLong()
                    viewModel.seekTo(targetMs)
                },
                colors = SliderDefaults.colors(
                    thumbColor = theme.primary,
                    activeTrackColor = theme.primary,
                    inactiveTrackColor = theme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .testTag("scrub_bar")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val effectivePosMs = if (isScrubbing) (scrubValue * durationMs).toLong() else currentPosMs
                val curMin = (effectivePosMs / 1000) / 60
                val curSec = (effectivePosMs / 1000) % 60
                Text(
                    text = "%d:%02d".format(curMin, curSec),
                    fontSize = 11.sp,
                    color = theme.subtextColor
                )

                val durMin = (durationMs / 1000) / 60
                val durSec = (durationMs / 1000) % 60
                Text(
                    text = "%d:%02d".format(durMin, durSec),
                    fontSize = 11.sp,
                    color = theme.subtextColor
                )
            }
        }

        // Playback Transport Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Shuffle
            IconButton(
                onClick = { viewModel.toggleShuffle() },
                modifier = Modifier.testTag("button_shuffle")
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffle) theme.primary else theme.subtextColor
                )
            }

            // Previous
            IconButton(
                onClick = { viewModel.previous() },
                modifier = Modifier.size(44.dp).testTag("button_prev")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = theme.textColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Play / Pause Floating Center Button
            FilledIconButton(
                onClick = { viewModel.togglePlayPause() },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = theme.primary),
                modifier = Modifier.size(56.dp).testTag("button_play_pause")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = theme.background,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Next
            IconButton(
                onClick = { viewModel.next() },
                modifier = Modifier.size(44.dp).testTag("button_next")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = theme.textColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Repeat
            IconButton(
                onClick = { viewModel.toggleRepeat() },
                modifier = Modifier.testTag("button_repeat")
            ) {
                val icon = when (repeatMode) {
                    SongRepeatMode.ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Repeat",
                    tint = if (repeatMode != SongRepeatMode.OFF) theme.primary else theme.subtextColor
                )
            }

            // Favorite
            IconButton(
                onClick = {
                    currentSong?.let { viewModel.toggleFavorite(it) }
                },
                modifier = Modifier.testTag("button_favorite")
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color.Red else theme.subtextColor
                )
            }
        }
    }
}
