package com.example.musicplayer

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem

class MusicService : Service() {

    companion object {
        const val ACTION_SONG_CHANGED = "com.example.musicplayer.SONG_CHANGED"
    }

    private var exoPlayer: ExoPlayer? = null
    private val binder = MusicBinder()
    private var currentSong: Song? = null
    private var playlist: List<Song> = emptyList()
    private var currentSongIndex = -1

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    fun playSong(song: Song, songs: List<Song>, index: Int) {
        this.playlist = songs
        this.currentSongIndex = index
        this.currentSong = song
        val mediaItem = MediaItem.fromUri(android.net.Uri.parse(song.path))
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
        exoPlayer?.play()
        val intent = Intent(ACTION_SONG_CHANGED)
        intent.putExtra("songIndex", currentSongIndex)
        sendBroadcast(intent)
    }

    fun playNext() {
        if (playlist.isNotEmpty()) {
            currentSongIndex = (currentSongIndex + 1) % playlist.size
            playSong(playlist[currentSongIndex], playlist, currentSongIndex)
        }
    }

    fun playPrevious() {
        if (playlist.isNotEmpty()) {
            currentSongIndex = if (currentSongIndex > 0) currentSongIndex - 1 else playlist.size - 1
            playSong(playlist[currentSongIndex], playlist, currentSongIndex)
        }
    }

    fun pauseSong() {
        exoPlayer?.pause()
    }

    fun resumeSong() {
        exoPlayer?.play()
    }

    fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying ?: false
    }

    fun getCurrentSong(): Song? {
        return currentSong
    }

    fun getDuration(): Long {
        return exoPlayer?.duration ?: 0
    }

    fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0
    }

    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }
}
