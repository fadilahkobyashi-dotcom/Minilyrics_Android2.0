package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.components.MiniLyricsHeader
import com.example.ui.dialogs.*
import com.example.ui.screens.*
import com.example.ui.theme.MiniLyricsTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val theme by viewModel.preferencesManager.themeState.collectAsState()
            val activeTab by viewModel.activeTab.collectAsState()

            // Runtime Permission Launcher for Android 16 / Android 13+ audio & notifications
            val permissionsToRequest = remember {
                val list = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    list.add(Manifest.permission.READ_MEDIA_AUDIO)
                    list.add(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                list.toTypedArray()
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { results ->
                val audioGranted = results[Manifest.permission.READ_MEDIA_AUDIO] == true ||
                        results[Manifest.permission.READ_EXTERNAL_STORAGE] == true
                if (audioGranted) {
                    viewModel.refreshLibrary()
                }
            }

            LaunchedEffect(Unit) {
                val hasAudioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
                } else {
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                }

                if (!hasAudioPermission) {
                    permissionLauncher.launch(permissionsToRequest)
                }
            }

            MiniLyricsTheme(themeOption = theme) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        MiniLyricsHeader(
                            currentTab = activeTab,
                            theme = theme,
                            onTabSelected = { viewModel.setActiveTab(it) },
                            onOpenEqualizer = { viewModel.setEqualizerDialogVisible(true) },
                            onOpenThemes = { viewModel.setThemeDialogVisible(true) },
                            onOpenSettings = { viewModel.setSettingsDialogVisible(true) }
                        )
                    },
                    bottomBar = {
                        // Mini player bar visible when browsing Songs, Artists, Albums, or Playlists
                        if (activeTab != MainTab.PLAYBACK) {
                            MiniPlayerBar(
                                viewModel = viewModel,
                                theme = theme,
                                onExpandPlayback = { viewModel.setActiveTab(MainTab.PLAYBACK) }
                            )
                        }
                    },
                    containerColor = theme.background
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (activeTab) {
                            MainTab.PLAYBACK -> {
                                PlaybackScreen(viewModel = viewModel, theme = theme)
                            }
                            MainTab.SONGS -> {
                                SongsScreen(viewModel = viewModel, theme = theme)
                            }
                            MainTab.ARTISTS -> {
                                ArtistsAlbumsScreen(viewModel = viewModel, theme = theme, isAlbumsMode = false)
                            }
                            MainTab.ALBUMS -> {
                                ArtistsAlbumsScreen(viewModel = viewModel, theme = theme, isAlbumsMode = true)
                            }
                            MainTab.PLAYLISTS -> {
                                PlaylistsScreen(viewModel = viewModel, theme = theme)
                            }
                        }
                    }

                    // BackHandler for secondary tabs (returns to Playback tab)
                    if (activeTab != MainTab.PLAYBACK) {
                        BackHandler {
                            viewModel.setActiveTab(MainTab.PLAYBACK)
                        }
                    }

                    // Dialogs
                    val showOnlineSearch by viewModel.showOnlineSearchDialog.collectAsState()
                    if (showOnlineSearch) {
                        OnlineLyricsSearchDialog(
                            viewModel = viewModel,
                            theme = theme,
                            onDismiss = { viewModel.closeSearchLyricsDialog() }
                        )
                    }

                    val showEditTrack by viewModel.showEditTrackDialog.collectAsState()
                    val editingSong by viewModel.editingSong.collectAsState()
                    if (showEditTrack && editingSong != null) {
                        TrackInfoEditDialog(
                            song = editingSong!!,
                            viewModel = viewModel,
                            theme = theme,
                            onDismiss = { viewModel.closeEditTrackDialog() }
                        )
                    }

                    val showEqualizer by viewModel.showEqualizerDialog.collectAsState()
                    if (showEqualizer) {
                        EqualizerDialog(
                            viewModel = viewModel,
                            theme = theme,
                            onDismiss = { viewModel.setEqualizerDialogVisible(false) }
                        )
                    }

                    val showThemes by viewModel.showThemeDialog.collectAsState()
                    if (showThemes) {
                        ThemeSelectorDialog(
                            viewModel = viewModel,
                            theme = theme,
                            onDismiss = { viewModel.setThemeDialogVisible(false) }
                        )
                    }

                    val showSettings by viewModel.showSettingsDialog.collectAsState()
                    if (showSettings) {
                        SettingsDialog(
                            viewModel = viewModel,
                            theme = theme,
                            onDismiss = { viewModel.setSettingsDialogVisible(false) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiniPlayerBar(
    viewModel: MainViewModel,
    theme: com.example.model.ThemeOption,
    onExpandPlayback: () -> Unit
) {
    val currentSong by (viewModel.playbackService?.currentSong ?: MutableStateFlow(null)).collectAsState()
    val isPlaying by (viewModel.playbackService?.isPlaying ?: MutableStateFlow(false)).collectAsState()

    if (currentSong != null) {
        Surface(
            color = theme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .clickable { onExpandPlayback() }
                .testTag("mini_player_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Album Art Thumbnail
                val embeddedArt = currentSong!!.embeddedArt
                val miniBitmap = remember(embeddedArt) {
                    if (embeddedArt != null && embeddedArt.isNotEmpty()) {
                        BitmapFactory.decodeByteArray(embeddedArt, 0, embeddedArt.size)
                    } else null
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(theme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (miniBitmap != null) {
                        Image(
                            bitmap = miniBitmap.asImageBitmap(),
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
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong!!.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentSong!!.artist,
                        fontSize = 11.sp,
                        color = theme.subtextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier.testTag("mini_button_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = theme.primary
                        )
                    }

                    IconButton(
                        onClick = { viewModel.next() },
                        modifier = Modifier.testTag("mini_button_next")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = theme.textColor
                        )
                    }
                }
            }
        }
    }
}
