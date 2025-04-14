package com.example.lab4

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri

interface MediaControllerInterface {
    fun play(uri: Uri)
    fun pause()
    fun stop()
}

class VideoPlayerController(
    private val videoView: VideoView,
    private val context: Context
) : MediaControllerInterface {
    private var currentUri: Uri? = null

    override fun play(uri: Uri) {
        if (currentUri == null || videoView.duration <= 0) {
            currentUri = uri
            videoView.setVideoURI(uri)
            val mediaController = MediaController(context)
            mediaController.setAnchorView(videoView)
            videoView.setMediaController(mediaController)
        }
        videoView.start()
    }

    override fun pause() {
        videoView.pause()
    }

    override fun stop() {
        videoView.stopPlayback()
        currentUri = null
    }
}

class AudioPlayerController(private val context: Context) : MediaControllerInterface {
    private var mediaPlayer: MediaPlayer? = null
    private var currentUri: Uri? = null

    override fun play(uri: Uri) {
        if (mediaPlayer == null) {
            currentUri = uri
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                prepare()
                start()
            }
        } else {
            try {
                mediaPlayer?.start()
            } catch (e: IllegalStateException) {
                stop()
                play(uri)
            }
        }
    }

    override fun pause() {
        mediaPlayer?.pause()
    }

    override fun stop() {
        mediaPlayer?.let {
            try {
                it.stop()
            } catch (e: IllegalStateException) { }
            it.reset()
            it.release()
        }
        mediaPlayer = null
        currentUri = null
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var btnChooseFile: Button
    private lateinit var btnLoadUrl: Button
    private lateinit var btnPlay: Button
    private lateinit var btnPause: Button
    private lateinit var btnStop: Button
    private lateinit var radioGroup: RadioGroup
    private lateinit var radioAudio: RadioButton
    private lateinit var radioVideo: RadioButton

    private var currentUri: Uri? = null
    private var isVideoFile: Boolean = true

    private lateinit var filePickerLauncher: ActivityResultLauncher<String>
    private lateinit var videoController: VideoPlayerController
    private lateinit var audioController: AudioPlayerController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        videoView = findViewById(R.id.video_view)
        btnChooseFile = findViewById(R.id.btnChooseFile)
        btnLoadUrl = findViewById(R.id.btnLoadUrl)
        btnPlay = findViewById(R.id.btnPlay)
        btnPause = findViewById(R.id.btnPause)
        btnStop = findViewById(R.id.btnStop)
        radioGroup = findViewById(R.id.radioGroup)
        radioAudio = findViewById(R.id.radioAudio)
        radioVideo = findViewById(R.id.radioVideo)

        videoController = VideoPlayerController(videoView, this)
        audioController = AudioPlayerController(this)

        filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                currentUri = it
                Toast.makeText(this, "Файл обрано", Toast.LENGTH_SHORT).show()
            }
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            isVideoFile = (checkedId == R.id.radioVideo)
            videoView.visibility = if (isVideoFile) VideoView.VISIBLE else VideoView.GONE
            if (!isVideoFile && videoView.isPlaying) {
                videoController.stop()
            }
        }

        btnChooseFile.setOnClickListener {
            val mimeType = if (isVideoFile) "video/*" else "audio/*"
            filePickerLauncher.launch(mimeType)
        }

        btnLoadUrl.setOnClickListener {
            showUrlInputDialog()
        }

        btnPlay.setOnClickListener {
            currentUri?.let { uri ->
                if (isVideoFile) {
                    if (videoView.duration > 0) {
                        videoView.start()
                    } else {
                        videoController.play(uri)
                    }
                } else {
                    audioController.play(uri)
                }
            } ?: Toast.makeText(this, "Файл не обрано", Toast.LENGTH_SHORT).show()
        }

        btnPause.setOnClickListener {
            if (isVideoFile) {
                videoController.pause()
            } else {
                audioController.pause()
            }
        }

        btnStop.setOnClickListener {
            if (isVideoFile) {
                if (currentUri != null) {
                    videoController.stop()
                    currentUri = null
                }
            } else {
                if (currentUri != null) {
                    audioController.stop()
                    currentUri = null
                }
            }
        }
    }

    private fun showUrlInputDialog() {
        val input = EditText(this).apply { hint = "Введіть URL файлу" }
        AlertDialog.Builder(this)
            .setTitle("Завантаження файлу з Інтернету")
            .setView(input)
            .setPositiveButton("Завантажити") { dialog, _ ->
                val urlText = input.text.toString()
                if (urlText.isNotEmpty()) {
                    currentUri = urlText.toUri()
                    if (isVideoFile) {
                        videoController.play(currentUri!!)
                    } else {
                        audioController.play(currentUri!!)
                    }
                } else {
                    Toast.makeText(this, "URL не може бути порожнім", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Скасувати") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioController.stop()
    }
}
