package me.zeeroooo.materialfb.activity

import android.Manifest
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import butterknife.BindView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.github.chrisbanes.photoview.OnPhotoTapListener
import com.github.chrisbanes.photoview.PhotoView
import me.zeeroooo.materialfb.R
import me.zeeroooo.materialfb.misc.Constant.COUNT_DOWN_FUTURE
import me.zeeroooo.materialfb.misc.Constant.COUNT_DOWN_INTERVAL
import me.zeeroooo.materialfb.misc.Constant.REQUEST_AUTHORIZE_CODE
import me.zeeroooo.materialfb.ui.CookingAToast
import java.io.File

class Photo : ButterKnifeActivity(R.layout.activity_photo), OnPhotoTapListener {

    @BindView(R.id.photo) lateinit var imageView: PhotoView
    @BindView(R.id.photoViewerTopGradient) lateinit var topGradient: View
    @BindView(R.id.toolbar_ph) lateinit var toolbar: Toolbar
    @BindView(R.id.photo_title) lateinit var imageTitle: TextView
    @BindView(android.R.id.progress) lateinit var progressBar: ProgressBar

    private lateinit var downloadManager: DownloadManager
    private lateinit var webView: WebView

    private var shareTarget: Target<Bitmap>? = null
    private var download = false
    private var shareFlag = 0
    private var imageUrl: String? = null

    override fun create(savedInstanceState: Bundle?) {
        downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        webView = WebView(this)

        imageView.setOnPhotoTapListener(this)
        imageTitle.text = intent.getStringExtra("title")

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        webView.settings.blockNetworkImage = true
        webView.settings.setAppCacheEnabled(false)
        webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        webView.loadUrl(intent.getStringExtra("link"))
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                imageUrl = url
                load()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val window = window
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    override fun onPhotoTap(view: ImageView, x: Float, y: Float) {
        updateButtonsVisibilityWithAnimation(View.VISIBLE, android.R.anim.fade_in)
        startCountDownToHideButtons()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.photo, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.download_image -> {
                download = true
                requestStoragePermission()
            }
            R.id.share_image -> {
                shareFlag = SHARE_IMAGE
                requestStoragePermission()
            }
            R.id.copy_url_image -> {
                val clipboard = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newUri(this.contentResolver, "", Uri.parse(imageUrl))
                clipboard.primaryClip = clip
                CookingAToast.cooking(this@Photo, getString(R.string.content_copy_link_done), Color.WHITE, Color.parseColor("#00C851"), R.drawable.ic_copy_url, true).show()
            }
            android.R.id.home -> onBackPressed()
        }
        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_AUTHORIZE_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (shareFlag == SHARE_IMAGE) {
                        shareImage()
                    } else if (download) {
                        // Save the image
                        val request = DownloadManager.Request(Uri.parse(imageUrl))

                        // Set the download directory
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/MaterialFBook")
                        if (!downloadsDir.exists())
                            downloadsDir.mkdir()
                        val destinationFile = File(downloadsDir, Uri.parse(imageUrl).lastPathSegment)
                        request.setDestinationUri(Uri.fromFile(destinationFile))

                        // Make notification stay after download
                        request.setVisibleInDownloadsUi(true)
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                        // Start the download
                        downloadManager.enqueue(request)

                        CookingAToast.cooking(this, getString(R.string.downloaded), Color.WHITE, Color.parseColor("#00C851"), R.drawable.ic_download, false).show()
                        download = false
                    }
                } else
                    CookingAToast.cooking(this, getString(R.string.permission_denied), Color.WHITE, Color.parseColor("#ff4444"), R.drawable.ic_error, true).show()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (shareFlag != SHARE_FLAG_DONE) {
            if (shareTarget != null)
                Glide.with(this@Photo).clear(shareTarget)
            imageView.setImageDrawable(null)
        }
        webView.clearCache(true)
        webView.clearHistory()
        webView.removeAllViews()
        webView.destroy()
    }

    private fun load() {
        Glide.with(this)
                .load(imageUrl)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                        return false
                    }

                    override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                        progressBar.visibility = View.GONE
                        startCountDownToHideButtons()
                        return false
                    }
                })
                .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE))
                .into(imageView)
    }

    private fun shareImage() {
        shareTarget = object : SimpleTarget<Bitmap>() {
            override fun onResourceReady(bitmap: Bitmap, transition: Transition<in Bitmap>?) {
                val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, Uri.parse(imageUrl).lastPathSegment, null)
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "image/*"
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(path))
                startActivity(Intent.createChooser(shareIntent, getString(R.string.context_share_image)))
                CookingAToast.cooking(this@Photo, getString(R.string.context_share_image_progress), Color.WHITE, Color.parseColor("#00C851"), R.drawable.ic_share, false).show()
            }
        }
        Glide.with(this@Photo).asBitmap().load(imageUrl).apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE)).into(shareTarget!!)
        shareFlag = SHARE_FLAG_DONE
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_AUTHORIZE_CODE)
    }

    private fun startCountDownToHideButtons() {
        var countdown = false
        val countDownTimer = object : CountDownTimer(COUNT_DOWN_FUTURE, COUNT_DOWN_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                countdown = true
            }

            override fun onFinish() {
                updateButtonsVisibilityWithAnimation(View.INVISIBLE, android.R.anim.fade_out)
                countdown = false
            }
        }
        if (!countdown)
            countDownTimer.start()
        else
            countDownTimer.cancel()
    }

    private fun updateButtonsVisibilityWithAnimation(visibility: Int, a: Int) {
        val animation = AnimationUtils.loadAnimation(this, a)

        topGradient.startAnimation(animation)
        toolbar.startAnimation(animation)
        imageTitle.startAnimation(animation)

        topGradient.visibility = visibility
        toolbar.visibility = visibility
        imageTitle.visibility = visibility
    }

    companion object {
        private const val SHARE_IMAGE = 1
        private const val SHARE_FLAG_DONE = 2
    }
}