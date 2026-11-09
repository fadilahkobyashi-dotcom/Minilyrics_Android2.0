package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Song
import com.example.model.ThemeOption
import com.example.ui.MainViewModel

@Composable
fun ArtistsAlbumsScreen(
    viewModel: MainViewModel,
    theme: ThemeOption,
    isAlbumsMode: Boolean,
    modifier: Modifier = Modifier
) {
    val itemsList by (if (isAlbumsMode) viewModel.albumsList else viewModel.artistsList).collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = if (isAlbumsMode) "Albums (${itemsList.size})" else "Artists (${itemsList.size})",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textColor,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(itemsList) { (groupName, songs) ->
                var expanded by remember { mutableStateOf(false) }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = theme.surface),
                    modifier = Modifier.fillMaxWidth().testTag("group_${groupName.lowercase().replace(" ", "_")}")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = !expanded }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (isAlbumsMode) Icons.Default.Album else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = theme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text(
                                        text = groupName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.textColor
                                    )
                                    Text(
                                        text = "${songs.size} tracks",
                                        fontSize = 12.sp,
                                        color = theme.subtextColor
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.playSongFromList(songs, 0) }) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play All",
                                        tint = theme.primary
                                    )
                                }
                                Icon(
                                    imageVector = if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = theme.subtextColor
                                )
                            }
                        }

                        if (expanded) {
                            HorizontalDivider(color = theme.surfaceVariant, thickness = 1.dp)
                            Column(modifier = Modifier.padding(8.dp)) {
                                songs.forEachIndexed { idx, song ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { viewModel.playSongFromList(songs, idx) }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${idx + 1}. ${song.title}",
                                            fontSize = 13.sp,
                                            color = theme.textColor,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = song.durationFormatted,
                                            fontSize = 12.sp,
                                            color = theme.subtextColor
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
}
