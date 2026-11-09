package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.LyricsSearchResult
import com.example.model.ThemeOption
import com.example.ui.MainViewModel

@Composable
fun OnlineLyricsSearchDialog(
    viewModel: MainViewModel,
    theme: ThemeOption,
    onDismiss: () -> Unit
) {
    val results by viewModel.onlineSearchResults.collectAsState()
    val isSearching by viewModel.isSearchingOnline.collectAsState()
    val currentSong = viewModel.playbackService?.currentSong?.value

    var titleQuery by remember { mutableStateOf(currentSong?.title ?: "") }
    var artistQuery by remember { mutableStateOf(currentSong?.artist ?: "") }

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
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Search Online Lyrics",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textColor
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = theme.subtextColor)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Inputs
                OutlinedTextField(
                    value = titleQuery,
                    onValueChange = { titleQuery = it },
                    label = { Text("Song Title", fontSize = 12.sp, color = theme.subtextColor) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.primary,
                        unfocusedBorderColor = theme.surfaceVariant,
                        focusedTextColor = theme.textColor,
                        unfocusedTextColor = theme.textColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_lyrics_title")
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = artistQuery,
                    onValueChange = { artistQuery = it },
                    label = { Text("Artist (Optional)", fontSize = 12.sp, color = theme.subtextColor) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.primary,
                        unfocusedBorderColor = theme.surfaceVariant,
                        focusedTextColor = theme.textColor,
                        unfocusedTextColor = theme.textColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_lyrics_artist")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { viewModel.searchOnlineLyrics(titleQuery, artistQuery) },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                    modifier = Modifier.fillMaxWidth().testTag("button_execute_search")
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = theme.background)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Search", color = theme.background, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(color = theme.surfaceVariant, thickness = 1.dp)

                Spacer(modifier = Modifier.height(8.dp))

                // Results list
                if (isSearching) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = theme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Searching online lyric sources...", fontSize = 13.sp, color = theme.subtextColor)
                        }
                    }
                } else if (results.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No results found.\nTry editing title or artist keywords above.",
                            fontSize = 13.sp,
                            color = theme.subtextColor,
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    Text(
                        text = "${results.size} lyrics found (Tap to apply & save):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.subtextColor,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(results) { item ->
                            LyricsResultCard(
                                result = item,
                                theme = theme,
                                onSelect = { viewModel.applyLyricsResult(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LyricsResultCard(
    result: LyricsSearchResult,
    theme: ThemeOption,
    onSelect: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = theme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("result_item_${result.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = result.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Synced Badge
                Surface(
                    color = if (result.isSynced) Color(0xFF10B981) else Color(0xFF6B7280),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (result.isSynced) "SYNCED LRC" else "PLAIN TXT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${result.artist}${if (result.album.isNotBlank()) " • " + result.album else ""}",
                fontSize = 12.sp,
                color = theme.subtextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Source: ${result.source}",
                        fontSize = 11.sp,
                        color = theme.primary
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(12.dp))
                        Text(text = "%.1f".format(result.rating), fontSize = 11.sp, color = theme.subtextColor)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = theme.subtextColor, modifier = Modifier.size(12.dp))
                        Text(text = "${result.downloads}", fontSize = 11.sp, color = theme.subtextColor)
                    }
                }

                Text(
                    text = "Apply & Save",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.primary
                )
            }
        }
    }
}
