package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Song
import com.example.model.ThemeOption
import com.example.ui.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun PlaylistsScreen(
    viewModel: MainViewModel,
    theme: ThemeOption,
    modifier: Modifier = Modifier
) {
    val allSongs by viewModel.songs.collectAsState()
    val favoriteIds by viewModel.favoriteSongIds.collectAsState()

    val favoriteSongs = remember(allSongs, favoriteIds) {
        allSongs.filter { favoriteIds.contains(it.id) }
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: Favorites, 1: All Queue

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                label = { Text("Favorites (${favoriteSongs.size})") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = if (selectedTab == 0) Color.Red else theme.subtextColor,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = theme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = theme.primary
                ),
                modifier = Modifier.testTag("tab_favorites")
            )

            val currentQueue by (viewModel.playbackService?.queue ?: MutableStateFlow(emptyList<Song>())).collectAsState()
            FilterChip(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                label = { Text("Current Queue (${currentQueue.size})") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = null,
                        tint = if (selectedTab == 1) theme.primary else theme.subtextColor,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = theme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = theme.primary
                ),
                modifier = Modifier.testTag("tab_queue")
            )
        }

        val displayList = if (selectedTab == 0) favoriteSongs else (viewModel.playbackService?.queue?.value ?: emptyList())

        if (displayList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (selectedTab == 0) "No favorites added yet" else "Playback queue is empty",
                    fontSize = 14.sp,
                    color = theme.subtextColor
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${displayList.size} tracks",
                    fontSize = 12.sp,
                    color = theme.subtextColor
                )
                Button(
                    onClick = { viewModel.playSongFromList(displayList, 0) },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp).testTag("button_play_all_playlist")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = theme.background, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play All", fontSize = 12.sp, color = theme.background)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                itemsIndexed(displayList) { index, song ->
                    SongItemRow(
                        song = song,
                        isCurrent = viewModel.playbackService?.currentSong?.value?.id == song.id,
                        isPlaying = viewModel.playbackService?.isPlaying?.value == true,
                        isFavorite = favoriteIds.contains(song.id),
                        theme = theme,
                        onSongClick = { viewModel.playSongFromList(displayList, index) },
                        onSearchLyrics = { viewModel.openSearchLyricsDialog(song) },
                        onEditTrack = { viewModel.openEditTrackDialog(song) },
                        onToggleFavorite = { viewModel.toggleFavorite(song) }
                    )
                }
            }
        }
    }
}
