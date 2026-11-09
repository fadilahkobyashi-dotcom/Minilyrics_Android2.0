package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PreferencesManager
import com.example.lyrics.LrcParser
import com.example.lyrics.LyricsOnlineSearchService
import com.example.lyrics.TitleSanitizer
import com.example.model.*
import com.example.repository.MusicRepository
import com.example.service.MediaPlaybackService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class MainTab(val title: String) {
    PLAYBACK("Playback"),
    SONGS("Songs"),
    ARTISTS("Artists"),
    ALBUMS("Albums"),
    PLAYLISTS("Playlists")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    val preferencesManager = PreferencesManager(application)
    private val lyricsSearchService = LyricsOnlineSearchService()

    var playbackService: MediaPlaybackService? = null
        private set

    private val _isServiceBound = MutableStateFlow(false)
    val isServiceBound = _isServiceBound.asStateFlow()

    // Navigation state
    private val _activeTab = MutableStateFlow(MainTab.PLAYBACK)
    val activeTab = _activeTab.asStateFlow()

    // Music Library
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs = _songs.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Lyrics State
    private val _currentLyrics = MutableStateFlow(LyricsData())
    val currentLyrics = _currentLyrics.asStateFlow()

    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics = _isLoadingLyrics.asStateFlow()

    // Online Lyrics Search Dialog
    private val _showOnlineSearchDialog = MutableStateFlow(false)
    val showOnlineSearchDialog = _showOnlineSearchDialog.asStateFlow()

    private val _onlineSearchResults = MutableStateFlow<List<LyricsSearchResult>>(emptyList())
    val onlineSearchResults = _onlineSearchResults.asStateFlow()

    private val _isSearchingOnline = MutableStateFlow(false)
    val isSearchingOnline = _isSearchingOnline.asStateFlow()

    // Track Info Edit Dialog
    private val _showEditTrackDialog = MutableStateFlow(false)
    val showEditTrackDialog = _showEditTrackDialog.asStateFlow()
    private val _editingSong = MutableStateFlow<Song?>(null)
    val editingSong = _editingSong.asStateFlow()

    // Dialogs
    private val _showEqualizerDialog = MutableStateFlow(false)
    val showEqualizerDialog = _showEqualizerDialog.asStateFlow()

    private val _showThemeDialog = MutableStateFlow(false)
    val showThemeDialog = _showThemeDialog.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog = _showSettingsDialog.asStateFlow()

    // Filtered lists
    val filteredSongs = combine(_songs, _searchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true) ||
                    it.album.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artistsList = _songs.map { list ->
        list.groupBy { it.artist }.map { (artist, songs) ->
            artist to songs
        }.sortedBy { it.first }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albumsList = _songs.map { list ->
        list.groupBy { it.album }.map { (album, songs) ->
            album to songs
        }.sortedBy { it.first }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Favorites
    val favoriteSongIds = repository.getFavoriteSongIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Service Connection
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? MediaPlaybackService.LocalBinder
            playbackService = binder?.getService()
            _isServiceBound.value = true
            observePlaybackService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService = null
            _isServiceBound.value = false
        }
    }

    init {
        bindPlaybackService()
        refreshLibrary()
    }

    private fun bindPlaybackService() {
        val intent = Intent(getApplication(), MediaPlaybackService::class.java)
        getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        getApplication<Application>().startService(intent)
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            val list = repository.getSongs()
            _songs.value = list
            if (playbackService?.queue?.value.isNullOrEmpty() && list.isNotEmpty()) {
                playbackService?.setQueue(list, 0)
                playbackService?.pause() // Ready on start without auto-blasting
            }
        }
    }

    private fun observePlaybackService() {
        val service = playbackService ?: return

        viewModelScope.launch {
            service.currentSong.collect { song ->
                if (song != null) {
                    loadLyricsForSong(song)
                    loadArtworkForSong(song)
                }
            }
        }
    }

    private fun loadArtworkForSong(song: Song) {
        if (song.embeddedArt != null) return
        viewModelScope.launch {
            val artBytes = repository.getAlbumArt(song)
            if (artBytes != null) {
                playbackService?.updateCurrentSongArt(artBytes)
            }
        }
    }

    fun loadLyricsForSong(song: Song) {
        viewModelScope.launch {
            _isLoadingLyrics.value = true
            val lyrics = repository.loadLyricsForSong(
                song = song,
                autoSearch = preferencesManager.autoSearchLyricsState.value
            )
            _currentLyrics.value = lyrics
            _isLoadingLyrics.value = false
        }
    }

    fun setActiveTab(tab: MainTab) {
        _activeTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun playSong(song: Song) {
        playbackService?.playSong(song)
        _activeTab.value = MainTab.PLAYBACK
    }

    fun playSongFromList(songsList: List<Song>, index: Int) {
        playbackService?.setQueue(songsList, index)
        _activeTab.value = MainTab.PLAYBACK
    }

    fun togglePlayPause() {
        playbackService?.togglePlayPause()
    }

    fun next() {
        playbackService?.next()
    }

    fun previous() {
        playbackService?.previous()
    }

    fun seekTo(positionMs: Long) {
        playbackService?.seekTo(positionMs)
    }

    fun toggleShuffle() {
        playbackService?.toggleShuffle()
    }

    fun toggleRepeat() {
        val current = playbackService?.repeatMode?.value ?: RepeatMode.ALL
        val nextMode = when (current) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        playbackService?.setRepeatMode(nextMode)
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song.id)
        }
    }

    // Lyrics timing calibration
    fun adjustLyricsOffset(deltaMs: Long) {
        val current = _currentLyrics.value
        val updated = LrcParser.applyOffset(current, deltaMs)
        _currentLyrics.value = updated

        val song = playbackService?.currentSong?.value
        if (song != null) {
            viewModelScope.launch {
                repository.updateLyricsOffset(song, updated.offsetMs)
            }
        }
    }

    fun resetLyricsOffset() {
        val current = _currentLyrics.value
        val updated = LrcParser.applyOffset(current, -current.offsetMs)
        _currentLyrics.value = updated
        val song = playbackService?.currentSong?.value
        if (song != null) {
            viewModelScope.launch {
                repository.updateLyricsOffset(song, 0L)
            }
        }
    }

    // Online Lyrics Search
    fun openSearchLyricsDialog(song: Song? = null) {
        val target = song ?: playbackService?.currentSong?.value
        _showOnlineSearchDialog.value = true
        if (target != null) {
            searchOnlineLyrics(target.title, target.artist)
        }
    }

    fun closeSearchLyricsDialog() {
        _showOnlineSearchDialog.value = false
    }

    fun searchOnlineLyrics(title: String, artist: String) {
        viewModelScope.launch {
            _isSearchingOnline.value = true
            val results = lyricsSearchService.searchLyrics(title, artist)
            _onlineSearchResults.value = results
            _isSearchingOnline.value = false
        }
    }

    fun applyLyricsResult(result: LyricsSearchResult) {
        val song = playbackService?.currentSong?.value ?: return
        viewModelScope.launch {
            val parsed = LrcParser.parse(result.lrcContent, result.source)
            _currentLyrics.value = parsed
            repository.saveLyricsForSong(song, result.lrcContent, result.source, 0L)
            closeSearchLyricsDialog()
        }
    }

    // Track metadata edit (solves messy filename problem!)
    fun openEditTrackDialog(song: Song) {
        _editingSong.value = song
        _showEditTrackDialog.value = true
    }

    fun closeEditTrackDialog() {
        _showEditTrackDialog.value = false
        _editingSong.value = null
    }

    fun saveTrackMetadata(song: Song, newTitle: String, newArtist: String, newAlbum: String) {
        viewModelScope.launch {
            repository.updateTrackInfo(song, newTitle, newArtist, newAlbum)
            closeEditTrackDialog()
            refreshLibrary()
            // Immediately search online with the corrected metadata
            searchOnlineLyrics(newTitle, newArtist)
            _showOnlineSearchDialog.value = true
        }
    }

    // Dialog Toggles
    fun setEqualizerDialogVisible(visible: Boolean) { _showEqualizerDialog.value = visible }
    fun setThemeDialogVisible(visible: Boolean) { _showThemeDialog.value = visible }
    fun setSettingsDialogVisible(visible: Boolean) { _showSettingsDialog.value = visible }

    override fun onCleared() {
        try {
            getApplication<Application>().unbindService(serviceConnection)
        } catch (ignored: Exception) {}
        super.onCleared()
    }
}
