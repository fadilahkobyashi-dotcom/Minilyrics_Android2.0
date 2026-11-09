package com.example.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.lyrics.TitleSanitizer
import com.example.model.Song
import com.example.model.ThemeOption
import com.example.ui.MainViewModel

@Composable
fun TrackInfoEditDialog(
    song: Song,
    viewModel: MainViewModel,
    theme: ThemeOption,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var album by remember { mutableStateOf(song.album) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = theme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Track Information",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textColor
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = theme.subtextColor)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Correcting the title and artist fixes messy filenames and helps MiniLyrics find the exact lyrics online.",
                    fontSize = 12.sp,
                    color = theme.subtextColor,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Song Title", color = theme.subtextColor) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.primary,
                        unfocusedBorderColor = theme.surfaceVariant,
                        focusedTextColor = theme.textColor,
                        unfocusedTextColor = theme.textColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_title")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist", color = theme.subtextColor) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.primary,
                        unfocusedBorderColor = theme.surfaceVariant,
                        focusedTextColor = theme.textColor,
                        unfocusedTextColor = theme.textColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_artist")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Album", color = theme.subtextColor) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.primary,
                        unfocusedBorderColor = theme.surfaceVariant,
                        focusedTextColor = theme.textColor,
                        unfocusedTextColor = theme.textColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_album")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Auto-Clean Button
                OutlinedButton(
                    onClick = {
                        val cleaned = TitleSanitizer.clean(title, artist, song.filePath)
                        title = cleaned.cleanTitle
                        artist = cleaned.cleanArtist
                    },
                    modifier = Modifier.fillMaxWidth().testTag("button_auto_clean")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = theme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto Clean Filename Junk", color = theme.primary, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Save & Search Lyrics Button
                Button(
                    onClick = {
                        viewModel.saveTrackMetadata(song, title, artist, album)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                    modifier = Modifier.fillMaxWidth().testTag("button_save_track_info")
                ) {
                    Text("Save & Search Lyrics", color = theme.background, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
