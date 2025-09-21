package com.example.musicplayer

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var songAdapter: SongAdapter
    private val songs = mutableListOf<Song>()
    private var musicService: MusicService? = null
    private var isBound = false
    private var currentSongIndex = -1

    private lateinit var playPauseButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var prevButton: ImageButton
    private lateinit var nowPlayingTitle: TextView
    private lateinit var nowPlayingArtist: TextView
    private lateinit var seekBar: SeekBar
    private val handler = android.os.Handler(Looper.getMainLooper())

    companion object {
        private const val REQUEST_CODE_READ_EXTERNAL_STORAGE = 1
    }

    private val openDocumentLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                loadSongFromUri(uri)
            }
            result.data?.clipData?.also { clipData ->
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    loadSongFromUri(uri)
                }
            }
            songAdapter.notifyDataSetChanged()
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true
            updateSeekBar()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    private val songChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MusicService.ACTION_SONG_CHANGED) {
                val songIndex = intent.getIntExtra("songIndex", -1)
                if (songIndex != -1) {
                    currentSongIndex = songIndex
                    val song = songs[songIndex]
                    nowPlayingTitle.text = song.title
                    nowPlayingArtist.text = song.artist
                    playPauseButton.setImageResource(R.drawable.ic_pause)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.playlist_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        songAdapter = SongAdapter(songs) { song ->
            playSong(song)
        }
        recyclerView.adapter = songAdapter

        val settingsButton: ImageButton = findViewById(R.id.settings_button)
        settingsButton.setOnClickListener {
            if (checkPermission()) {
                openImportDialog()
            } else {
                requestPermission()
            }
        }

        playPauseButton = findViewById(R.id.play_pause_button)
        nextButton = findViewById(R.id.next_button)
        prevButton = findViewById(R.id.prev_button)
        nowPlayingTitle = findViewById(R.id.now_playing_title)
        nowPlayingArtist = findViewById(R.id.now_playing_artist)

        playPauseButton.setOnClickListener {
            if (musicService?.isPlaying() == true) {
                musicService?.pauseSong()
                playPauseButton.setImageResource(R.drawable.ic_play)
            } else {
                musicService?.resumeSong()
                playPauseButton.setImageResource(R.drawable.ic_pause)
            }
        }

        nextButton.setOnClickListener {
            musicService?.playNext()
        }

        prevButton.setOnClickListener {
            musicService?.playPrevious()
        }

        seekBar = findViewById(R.id.seek_bar)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    musicService?.seekTo(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateSeekBar() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isBound) {
                    seekBar.max = musicService?.getDuration()?.toInt() ?: 0
                    seekBar.progress = musicService?.getCurrentPosition()?.toInt() ?: 0
                    handler.postDelayed(this, 1000)
                }
            }
        }, 1000)
    }

    override fun onStart() {
        super.onStart()
        Intent(this, MusicService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
        registerReceiver(songChangedReceiver, IntentFilter(MusicService.ACTION_SONG_CHANGED))
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        isBound = false
        unregisterReceiver(songChangedReceiver)
    }

    private fun playSong(song: Song) {
        val index = songs.indexOf(song)
        if (index != -1) {
            currentSongIndex = index
            musicService?.playSong(song, songs, index)
            nowPlayingTitle.text = song.title
            nowPlayingArtist.text = song.artist
            playPauseButton.setImageResource(R.drawable.ic_pause)
        }
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            REQUEST_CODE_READ_EXTERNAL_STORAGE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_READ_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImportDialog()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openImportDialog() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/mpeg", "audio/m4a"))
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        openDocumentLauncher.launch(intent)
    }

    private fun loadSongFromUri(uri: android.net.Uri) {
        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(this, uri)
            val title = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Unknown Title"
            val artist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
            val album = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown Album"
            val id = System.currentTimeMillis()
            val path = uri.toString()
            songs.add(Song(id, title, artist, album, path))
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            // Handle exceptions, e.g. file not found or invalid format
            Toast.makeText(this, "Error loading file: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            retriever.release()
        }
    }
}
