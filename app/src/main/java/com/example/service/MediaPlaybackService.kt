package com.example.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.MiniLyricsApp
import com.example.R
import com.example.model.RepeatMode
import com.example.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class MediaPlaybackService : Service(), MediaPlayer.OnPreparedListener,
    MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private val binder = LocalBinder()
    private var mediaPlayer: MediaPlayer? = null
    val equalizerController = EqualizerController()

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var positionJob: Job? = null
    private var sleepTimerJob: Job? = null

    // State Flows
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle = _isShuffle.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue = _queue.asStateFlow()

    private var currentIndex = -1
    private val shuffledIndices = mutableListOf<Int>()

    private val _sleepTimerSec = MutableStateFlow(0)
    val sleepTimerSec = _sleepTimerSec.asStateFlow()

    private var defaultArtwork: Bitmap? = null

    // Headset unplugged receiver
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pause()
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): MediaPlaybackService = this@MediaPlaybackService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        initMediaSession()
        registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        try {
            defaultArtwork = BitmapFactory.decodeResource(resources, R.drawable.default_album_art)
        } catch (ignored: Exception) {}
    }

    private fun initMediaSession() {
        mediaSession = MediaSession(this, "MiniLyricsMediaSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() { resume() }
                override fun onPause() { pause() }
                override fun onSkipToNext() { next() }
                override fun onSkipToPrevious() { previous() }
                override fun onSeekTo(pos: Long) { seekTo(pos) }
            })
            isActive = true
        }
    }

    private fun updateMediaSessionState(state: Int) {
        val actions = PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_SEEK_TO

        val playbackState = PlaybackState.Builder()
            .setActions(actions)
            .setState(state, _currentPosition.value, 1.0f)
            .build()
        mediaSession?.setPlaybackState(playbackState)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> resume()
            ACTION_PAUSE -> pause()
            ACTION_TOGGLE -> togglePlayPause()
            ACTION_NEXT -> next()
            ACTION_PREV -> previous()
            ACTION_STOP -> stopPlayback()
        }
        return START_NOT_STICKY
    }

    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        _queue.value = songs
        updateShuffledList(songs.size)
        if (startIndex in songs.indices) {
            currentIndex = startIndex
            playCurrentSong()
        }
    }

    private fun updateShuffledList(size: Int) {
        shuffledIndices.clear()
        shuffledIndices.addAll(0 until size)
        if (_isShuffle.value) {
            shuffledIndices.shuffle()
        }
    }

    fun playSong(song: Song) {
        val currentList = _queue.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == song.id }
        if (index >= 0) {
            currentIndex = index
        } else {
            currentList.add(song)
            _queue.value = currentList
            currentIndex = currentList.size - 1
            updateShuffledList(currentList.size)
        }
        playCurrentSong()
    }

    fun updateCurrentSongArt(art: ByteArray) {
        val current = _currentSong.value ?: return
        val updated = current.copy(embeddedArt = art)
        _currentSong.value = updated
        val list = _queue.value.toMutableList()
        val index = list.indexOfFirst { it.id == current.id }
        if (index >= 0) {
            list[index] = updated
            _queue.value = list
        }
        updateNotification()
    }

    private fun playCurrentSong() {
        val list = _queue.value
        if (currentIndex !in list.indices) return
        val song = list[currentIndex]
        _currentSong.value = song

        // Asynchronously extract embedded ID3 album art if not already present
        if (song.embeddedArt == null) {
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(applicationContext, song.contentUri)
                    val picture = retriever.embeddedPicture
                    retriever.release()
                    if (picture != null) {
                        withContext(Dispatchers.Main) {
                            if (_currentSong.value?.id == song.id) {
                                updateCurrentSongArt(picture)
                            }
                        }
                    } else if (song.albumArtUri != null) {
                        try {
                            contentResolver.openInputStream(song.albumArtUri)?.use { stream ->
                                val bytes = stream.readBytes()
                                if (bytes.isNotEmpty()) {
                                    withContext(Dispatchers.Main) {
                                        if (_currentSong.value?.id == song.id) {
                                            updateCurrentSongArt(bytes)
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                } catch (e: Exception) {
                    if (song.albumArtUri != null) {
                        try {
                            contentResolver.openInputStream(song.albumArtUri)?.use { stream ->
                                val bytes = stream.readBytes()
                                if (bytes.isNotEmpty()) {
                                    withContext(Dispatchers.Main) {
                                        if (_currentSong.value?.id == song.id) {
                                            updateCurrentSongArt(bytes)
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        }

        releasePlayer()

        try {
            mediaPlayer = MediaPlayer().apply {
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                setDataSource(applicationContext, song.contentUri)

                setOnPreparedListener(this@MediaPlaybackService)
                setOnCompletionListener(this@MediaPlaybackService)
                setOnErrorListener(this@MediaPlaybackService)
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("MediaPlaybackService", "Error setting data source: ${e.message}")
            // Advance to next song on unplayable file
            next()
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        mp?.start()
        _isPlaying.value = true
        _duration.value = mp?.duration?.toLong() ?: 0L
        _currentPosition.value = 0L

        equalizerController.attachToSession(mp?.audioSessionId ?: 0, true, 200)

        updateMediaSessionState(PlaybackState.STATE_PLAYING)
        startForeground(NOTIFICATION_ID, buildNotification(true))
        startPositionTracker()
    }

    override fun onCompletion(mp: MediaPlayer?) {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0)
                resume()
            }
            RepeatMode.ALL -> next()
            RepeatMode.OFF -> {
                if (currentIndex < _queue.value.size - 1) {
                    next()
                } else {
                    pause()
                    seekTo(0)
                }
            }
        }
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        Log.e("MediaPlaybackService", "MediaPlayer error: what=$what, extra=$extra")
        releasePlayer()
        _isPlaying.value = false
        updateNotification()
        return true
    }

    fun resume() {
        if (mediaPlayer == null && _currentSong.value != null) {
            playCurrentSong()
            return
        }
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _isPlaying.value = true
                updateMediaSessionState(PlaybackState.STATE_PLAYING)
                startForeground(NOTIFICATION_ID, buildNotification(true))
                startPositionTracker()
            }
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
                updateMediaSessionState(PlaybackState.STATE_PAUSED)
                stopForeground(STOP_FOREGROUND_DETACH)
                updateNotification()
                stopPositionTracker()
            }
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) pause() else resume()
    }

    fun next() {
        val list = _queue.value
        if (list.isEmpty()) return

        if (_isShuffle.value && shuffledIndices.isNotEmpty()) {
            val currentShuffledPos = shuffledIndices.indexOf(currentIndex)
            val nextShuffledPos = (currentShuffledPos + 1) % shuffledIndices.size
            currentIndex = shuffledIndices[nextShuffledPos]
        } else {
            currentIndex = (currentIndex + 1) % list.size
        }
        playCurrentSong()
    }

    fun previous() {
        val list = _queue.value
        if (list.isEmpty()) return

        // If played more than 3 seconds, replay song from beginning
        if (_currentPosition.value > 3000) {
            seekTo(0)
            return
        }

        if (_isShuffle.value && shuffledIndices.isNotEmpty()) {
            val currentShuffledPos = shuffledIndices.indexOf(currentIndex)
            val prevShuffledPos = if (currentShuffledPos - 1 < 0) shuffledIndices.size - 1 else currentShuffledPos - 1
            currentIndex = shuffledIndices[prevShuffledPos]
        } else {
            currentIndex = if (currentIndex - 1 < 0) list.size - 1 else currentIndex - 1
        }
        playCurrentSong()
    }

    fun seekTo(positionMs: Long) {
        val pos = positionMs.coerceIn(0L, _duration.value)
        mediaPlayer?.seekTo(pos.toInt())
        _currentPosition.value = pos
    }

    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
    }

    fun toggleShuffle() {
        val newShuffle = !_isShuffle.value
        _isShuffle.value = newShuffle
        updateShuffledList(_queue.value.size)
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerSec.value = 0
            return
        }
        _sleepTimerSec.value = minutes * 60
        sleepTimerJob = serviceScope.launch {
            while (_sleepTimerSec.value > 0) {
                delay(1000)
                _sleepTimerSec.value -= 1
            }
            pause()
        }
    }

    private fun startPositionTracker() {
        positionJob?.cancel()
        positionJob = serviceScope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        _currentPosition.value = it.currentPosition.toLong()
                    }
                }
                delay(200)
            }
        }
    }

    private fun stopPositionTracker() {
        positionJob?.cancel()
        positionJob = null
    }

    private fun buildNotification(isPlaying: Boolean): Notification {
        val song = _currentSong.value
        val title = song?.title ?: "MiniLyrics"
        val artist = song?.artist ?: "Ready to play"

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevIntent = PendingIntent.getService(
            this, 1, Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_IMMUTABLE
        )
        val toggleIntent = PendingIntent.getService(
            this, 2, Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_TOGGLE },
            PendingIntent.FLAG_IMMUTABLE
        )
        val nextIntent = PendingIntent.getService(
            this, 3, Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE
        )

        var artBitmap: Bitmap? = null
        if (song?.embeddedArt != null && song.embeddedArt.isNotEmpty()) {
            artBitmap = BitmapFactory.decodeByteArray(song.embeddedArt, 0, song.embeddedArt.size)
        }
        if (artBitmap == null) {
            artBitmap = defaultArtwork
        }

        return NotificationCompat.Builder(this, MiniLyricsApp.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_fg_img)
            .setLargeIcon(artBitmap)
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevIntent)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                toggleIntent
            )
            .addAction(android.R.drawable.ic_media_next, "Next", nextIntent)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(_isPlaying.value))
    }

    private fun releasePlayer() {
        stopPositionTracker()
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    try { player.stop() } catch (ignored: Exception) {}
                }
                player.reset()
                player.release()
            }
        } catch (ignored: Exception) {}
        mediaPlayer = null
    }

    private fun stopPlayback() {
        pause()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        unregisterReceiver(noisyReceiver)
        serviceScope.cancel()
        releasePlayer()
        equalizerController.release()
        mediaSession?.release()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_PLAY = "com.example.minilyrics.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.minilyrics.ACTION_PAUSE"
        const val ACTION_TOGGLE = "com.example.minilyrics.ACTION_TOGGLE"
        const val ACTION_NEXT = "com.example.minilyrics.ACTION_NEXT"
        const val ACTION_PREV = "com.example.minilyrics.ACTION_PREV"
        const val ACTION_STOP = "com.example.minilyrics.ACTION_STOP"
    }
}
