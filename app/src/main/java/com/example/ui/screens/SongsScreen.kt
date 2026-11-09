package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Song
import com.example.model.ThemeOption
import com.example.ui.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun SongsScreen(
    viewModel: MainViewModel,
    theme: ThemeOption,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.filteredSongs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val favoriteIds by viewModel.favoriteSongIds.collectAsState()

    val currentPlayingSong by (viewModel.playbackService?.currentSong ?: MutableStateFlow(null)).collectAsState()
    val isPlaying by (viewModel.playbackService?.isPlaying ?: MutableStateFlow(false)).collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
            .padding(horizontal = 12.dp)
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search title, artist, or album...", fontSize = 13.sp, color = theme.subtextColor) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = theme.subtextColor)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = theme.subtextColor)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = theme.primary,
                unfocusedBorderColor = theme.surfaceVariant,
                focusedTextColor = theme.textColor,
                unfocusedTextColor = theme.textColor,
                cursorColor = theme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .testTag("search_music_input")
        )

        // Track Count & Rescan Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${songs.size} tracks found",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = theme.subtextColor
            )

            TextButton(
                onClick = { viewModel.refreshLibrary() },
                modifier = Modifier.testTag("button_rescan_library")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Rescan",
                    tint = theme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Rescan", fontSize = 12.sp, color = theme.primary)
            }
        }

        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MusicOff,
                        contentDescription = "No Music",
                        tint = theme.subtextColor,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No songs matching your query",
                        fontSize = 15.sp,
                        color = theme.textColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                itemsIndexed(songs) { index, song ->
                    val isCurrent = currentPlayingSong?.id == song.id
                    val isFav = favoriteIds.contains(song.id)

                    SongItemRow(
                        song = song,
                        isCurrent = isCurrent,
                        isPlaying = isPlaying,
                        isFavorite = isFav,
                        theme = theme,
                        onSongClick = { viewModel.playSongFromList(songs, index) },
                        onSearchLyrics = { viewModel.openSearchLyricsDialog(song) },
                        onEditTrack = { viewModel.openEditTrackDialog(song) },
                        onToggleFavorite = { viewModel.toggleFavorite(song) }
                    )
                }
            }
        }
    }
}

@Composable
fun SongItemRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    theme: ThemeOption,
    onSongClick: () -> Unit,
    onSearchLyrics: () -> Unit,
    onEditTrack: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isCurrent) theme.primary.copy(alpha = 0.15f) else theme.surface)
            .clickable { onSongClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("song_item_${song.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail Album Art
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(theme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val embeddedArt = song.embeddedArt
            if (embeddedArt != null && embeddedArt.isNotEmpty()) {
                val bitmap = remember(embeddedArt) {
                    BitmapFactory.decodeByteArray(embeddedArt, 0, embeddedArt.size)
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.default_album_art),
                        contentDescription = "Cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Image(
                    painter = painterResource(id = R.drawable.default_album_art),
                    contentDescription = "Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isCurrent && isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = "Playing",
                        tint = theme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isCurrent) theme.primary else theme.textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${song.artist} • ${song.album}",
                fontSize = 12.sp,
                color = theme.subtextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Duration
        Text(
            text = song.durationFormatted,
            fontSize = 12.sp,
            color = theme.subtextColor
        )

        // Overflow Actions Menu
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = theme.subtextColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(theme.surface)
            ) {
                DropdownMenuItem(
                    text = { Text("Search Lyrics Online", color = theme.textColor) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = theme.primary) },
                    onClick = {
                        menuExpanded = false
                        onSearchLyrics()
                    }
                )

                DropdownMenuItem(
                    text = { Text("Edit Track Title & Artist", color = theme.textColor) },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = theme.textColor) },
                    onClick = {
                        menuExpanded = false
                        onEditTrack()
                    }
                )

                DropdownMenuItem(
                    text = { Text(if (isFavorite) "Remove from Favorites" else "Add to Favorites", color = theme.textColor) },
                    leadingIcon = {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorite) Color.Red else theme.textColor
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onToggleFavorite()
                    }
                )
            }
        }
    }
}
