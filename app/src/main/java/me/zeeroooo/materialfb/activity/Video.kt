package me.zeeroooo.materialfb.activity

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import butterknife.BindView
import me.zeeroooo.materialfb.R
import me.zeeroooo.materialfb.misc.Constant.COUNT_DOWN_FUTURE
import me.zeeroooo.materialfb.misc.Constant.COUNT_DOWN_INTERVAL
import me.zeeroooo.materialfb.misc.Constant.Preference.VIDEO_URL
import me.zeeroooo.materialfb.misc.Constant.REQUEST_AUTHORIZE_CODE
import me.zeeroooo.materialfb.ui.CookingAToast
import me.zeeroooo.materialfb.webview.VideoViewTouchable
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

class Video : ButterKnifeActivity(R.layout.activity_video) {

    @BindView(R.id.video_view) lateinit var videoView: VideoViewTouchable
    @BindView(R.id.buttons_header) lateinit var buttonsHeader: RelativeLayout
    @BindView(R.id.progress) lateinit var seekBar: SeekBar
    @BindView(R.id.elapsed_time) lateinit var elapsedTime: TextView
    @BindView(R.id.remaining_time) lateinit var remainingTime: TextView

    private lateinit var downloadManager: DownloadManager
    private lateinit var url: String
    private var position = 0

    override fun create(savedInstanceState: Bundle?) {
        url = intent.getStringExtra(VIDEO_URL)

        downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        seekBar.progressDrawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        seekBar.thumb.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)

        videoView.setVideoURI(Uri.parse(url))
        videoView.requestFocus()
        videoView.setOnPreparedListener { _ ->
            videoView.seekTo(position)
            seekBar.max = videoView.duration
            seekBar.postDelayed(update, 1000)
            elapsedTime.postDelayed(update, 1000)
            remainingTime.postDelayed(update, 1000)
            setButtonsHeaderVisibility(View.GONE, android.R.anim.fade_out)
            if (position == 0) {
                videoView.start()
            }
        }
        videoView.setOnTouchListener { _, _ ->
            startCountDownToHideButtons()
            setButtonsHeaderVisibility(View.VISIBLE, android.R.anim.fade_in)
            false
        }

        // Buttons
        val pause = findViewById<ImageButton>(R.id.pauseplay_btn)
        setButtonBackground(pause)
        pause.setOnClickListener { view ->
            val imageButton = view as ImageButton
            if (videoView.isPlaying) {
                videoView.pause()
                imageButton.setImageResource(android.R.drawable.ic_media_play)
            } else {
                videoView.start()
                imageButton.setImageResource(android.R.drawable.ic_media_pause)
            }
        }

        val previous = findViewById<ImageButton>(R.id.previous_btn)
        setButtonBackground(previous)
        previous.setOnClickListener { _ ->
            videoView.seekTo(0)
            seekBar.progress = 0
        }

        val download = findViewById<ImageButton>(R.id.download_btn)
        setButtonBackground(download)
        download.setOnClickListener { requestStoragePermission() }

        val share = findViewById<ImageButton>(R.id.share_btn)
        setButtonBackground(share)
        share.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, url)
            startActivity(Intent.createChooser(shareIntent, getString(R.string.context_share_link)))
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onStopTrackingTouch(seekBar: SeekBar) {}

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) videoView.seekTo(progress)
            }
        })
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val request = DownloadManager.Request(Uri.parse(url))

                    // Set the download directory
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES + "/MaterialFBook")
                    if (!downloadsDir.exists())
                        downloadsDir.mkdir()
                    val destinationFile = File(downloadsDir, Uri.parse(url).lastPathSegment)
                    request.setDestinationUri(Uri.fromFile(destinationFile))

                    // Make notification stay after download
                    request.setVisibleInDownloadsUi(true)
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                    // Start the download
                    downloadManager.enqueue(request)

                    CookingAToast.cooking(this, getString(R.string.downloaded), Color.WHITE, Color.parseColor("#00C851"), R.drawable.ic_download, false).show()
                } else
                    CookingAToast.cooking(this, getString(R.string.permission_denied), Color.WHITE, Color.parseColor("#ff4444"), R.drawable.ic_error, true).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        position = videoView.currentPosition
        videoView.pause()
    }

    override fun onResume() {
        super.onResume()
        videoView.seekTo(position)
        videoView.start()
    }

    private fun setButtonsHeaderVisibility(visibility: Int, a: Int) {
        val animation = AnimationUtils.loadAnimation(this, a)
        buttonsHeader.startAnimation(animation)
        buttonsHeader.visibility = visibility
    }

    private val update = object : Runnable {
        override fun run() {
            seekBar.progress = videoView.currentPosition
            if (videoView.isPlaying) {
                seekBar.postDelayed(this, 1000)
                elapsedTime.text = formatTime(videoView.currentPosition.toLong())
                remainingTime.text = formatTime((videoView.duration - videoView.currentPosition).toLong())
            }
        }
    }

    private fun formatTime(ms: Long): String {
        return String.format(Locale.getDefault(), "%d:%d", TimeUnit.MILLISECONDS.toMinutes(ms), TimeUnit.MILLISECONDS.toSeconds(ms) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(ms)))
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_AUTHORIZE_CODE)
    }

    private fun startCountDownToHideButtons() {
        object : CountDownTimer(COUNT_DOWN_FUTURE, COUNT_DOWN_INTERVAL) {

            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                setButtonsHeaderVisibility(View.INVISIBLE, android.R.anim.fade_out)
            }
        }.start()
    }

    private fun setButtonBackground(button: View) {
        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, typedValue, true)
        button.setBackgroundResource(typedValue.resourceId)
    }
}
