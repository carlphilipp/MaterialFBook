/*
 * Code taken from:
 * - FaceSlim by indywidualny. Thanks.
 * - Folio for Facebook by creativetrendsapps. Thanks.
 * - Toffed by JakeLane. Thanks.
 */
package me.zeeroooo.materialfb.activity

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.annotation.NonNull
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v4.widget.SwipeRefreshLayout
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.widget.ImageView
import android.widget.TextView
import butterknife.BindView
import com.github.clans.fab.FloatingActionMenu
import me.zeeroooo.materialfb.R
import me.zeeroooo.materialfb.listener.FabOnClickListener
import me.zeeroooo.materialfb.misc.Constant.INPUT_FILE_REQUEST_CODE
import me.zeeroooo.materialfb.misc.Constant.Preference.APPLY
import me.zeeroooo.materialfb.misc.Constant.Preference.JOB_URL
import me.zeeroooo.materialfb.misc.Constant.Preference.VIDEO_URL
import me.zeeroooo.materialfb.misc.Constant.Url.DESKTOP_URL
import me.zeeroooo.materialfb.misc.Constant.Url.MBASIC_FULL_URL
import me.zeeroooo.materialfb.misc.Constant.Url.MOBILE_FULL_URL
import me.zeeroooo.materialfb.misc.Constant.Url.MOBILE_URL
import me.zeeroooo.materialfb.misc.Constant.Url.WEB_URL
import me.zeeroooo.materialfb.misc.UserInfoAsyncTask
import me.zeeroooo.materialfb.misc.Utils
import me.zeeroooo.materialfb.ui.CookingAToast
import me.zeeroooo.materialfb.ui.Theme
import me.zeeroooo.materialfb.webview.Helpers
import me.zeeroooo.materialfb.webview.JavaScriptHelpers
import me.zeeroooo.materialfb.webview.JavaScriptInterfaces
import me.zeeroooo.materialfb.webview.MFBWebView
import me.zeeroooo.materialfb.webview.WebChromeClient
import me.zeeroooo.materialfb.webview.WebViewClient
import java.io.File
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

class MainActivity : ButterKnifeActivity(R.layout.activity_main), NavigationView.OnNavigationItemSelectedListener {

    private val preferencesService = App.instance.preferenceService

    @BindView(R.id.webview) lateinit var webView: MFBWebView
    @BindView(R.id.drawer_layout) lateinit var drawer: DrawerLayout
    @BindView(R.id.swipeLayout) lateinit var swipeView: SwipeRefreshLayout
    @BindView(R.id.menuFAB) lateinit var floatingActionMenu: FloatingActionMenu
    @BindView(R.id.nav_view) lateinit var navigationView: NavigationView

    private lateinit var profileNameTv: TextView
    private lateinit var coverIv: ImageView
    private lateinit var profilePictureIv: ImageView
    private lateinit var downloadManager: DownloadManager
    private lateinit var baseUrl: String

    var cameraPhotoPath: String? = null
    val css = StringBuilder()
    var url: String? = null
    private var urlIntent: String? = null
    var filePathCallback: ValueCallback<Array<Uri>>? = null
    var sharedFromGallery: Uri? = null
        private set
    private var badgeUpdateHandler: Handler? = null
    private var badgeTask: Runnable? = null

    override fun create(savedInstanceState: Bundle?) {
        Theme.applyTheme(this.applicationContext)

        profileNameTv = navigationView.getHeaderView(0).findViewById(R.id.profile_name)
        coverIv = navigationView.getHeaderView(0).findViewById(R.id.cover)
        profilePictureIv = navigationView.getHeaderView(0).findViewById(R.id.profile_picture)

        downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        setupColor()
        setupBaseUrl()
        loadWebViewUrl()
        setupNavigationView()
        setupSwipeView()
        setupWebView()
        setupFabListener()
        setupProfileImage()

        if (intent != null) {
            urlIntent(intent)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            INPUT_FILE_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val request = DownloadManager.Request(Uri.parse(url))
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists())
                        if (!downloadsDir.mkdirs())
                            return

                    val destinationFile = File(downloadsDir, Uri.parse(url).lastPathSegment)
                    request.setDestinationUri(Uri.fromFile(destinationFile))
                    request.setVisibleInDownloadsUi(true)
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    downloadManager.enqueue(request)
                    webView.goBack()
                    CookingAToast.cooking(this, getString(R.string.downloaded), Color.WHITE, Color.parseColor("#00C851"), R.drawable.ic_download, false).show()
                } else
                    CookingAToast.cooking(this, getString(R.string.permission_denied), Color.WHITE, Color.parseColor("#ff4444"), R.drawable.ic_error, true).show()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        setIntent(intent)
        if (intent.getBooleanExtra(APPLY, false)) {
            finish()
            val apply = Intent(this, MainActivity::class.java)
            startActivity(apply)
        }
        urlIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        webView.resumeTimers()

        if (Helpers.cookie != null && !preferencesService.shouldSaveData()) {
            badgeUpdateHandler = Handler()
            badgeTask = Runnable {
                JavaScriptHelpers.updateNumsService(webView)
                badgeUpdateHandler!!.postDelayed(badgeTask, 15000)
            }
            badgeTask!!.run()
            UserInfoAsyncTask(this, profileNameTv, coverIv, profilePictureIv).execute()
        }
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        if (badgeTask != null && badgeUpdateHandler != null)
            badgeUpdateHandler!!.removeCallbacks(badgeTask)
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.clearCache(true)
        webView.clearHistory()
        webView.removeAllViews()
        webView.destroy()
        if (badgeTask != null && badgeUpdateHandler != null) {
            badgeUpdateHandler!!.removeCallbacks(badgeTask)
        }
        if (preferencesService.shouldClearCache()) {
            Utils.deleteCache(this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Used when uploading a file
        // Thanks to Koras for the tutorial. http://dev.indywidualni.org/2015/02/an-advanced-webview-with-some-cool-features
        if (requestCode != INPUT_FILE_REQUEST_CODE || filePathCallback == null)
            return

        var results: Array<Uri>? = null

        // Check that the response is a good one
        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                // If there is not data, then we may have taken a photo
                if (cameraPhotoPath != null) {
                    results = arrayOf(Uri.parse(cameraPhotoPath))
                }
            } else {
                val dataString = data.dataString
                if (dataString != null)
                    results = arrayOf(Uri.parse(dataString))
            }
        }

        filePathCallback?.onReceiveValue(results)
        filePathCallback = null
    }

    override fun onBackPressed() {
        when {
            drawer.isDrawerOpen(GravityCompat.START) -> drawer.closeDrawers()
            webView.canGoBack() -> webView.goBack()
            else -> super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        setupBaseUrl()
        webView.stopLoading()
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_top_stories -> {
                webView.visibility = View.INVISIBLE

                webView.loadUrl("$baseUrl/home.php?sk=h_nor")
                setTitle(R.string.menu_top_stories)
                item.isChecked = true
            }
            R.id.nav_most_recent -> {
                webView.visibility = View.INVISIBLE

                webView.loadUrl("$baseUrl/home.php?sk=h_chr'")
                setTitle(R.string.menu_most_recent)
                item.isChecked = true
                Helpers.uncheckRadioMenu(navigationView.menu)
            }
            R.id.nav_friendreq -> {
                webView.visibility = View.INVISIBLE

                webView.loadUrl("$baseUrl/friends/center/requests/")
                setTitle(R.string.menu_friendreq)
                item.isChecked = true
            }
            R.id.nav_groups -> {
                webView.visibility = View.INVISIBLE

                webView.loadUrl("$baseUrl/groups/?category=membership")
                css.append("._129- {position:initial}")
                item.isChecked = true
            }
            R.id.nav_mainmenu -> {
                webView.visibility = View.INVISIBLE

                if (!preferencesService.shouldSaveData())
                    webView.loadUrl("javascript:(function()%7Btry%7Bdocument.querySelector('%23bookmarks_jewel%20%3E%20a').click()%7Dcatch(_)%7Bwindow.location.href%3D'" + "https%3A%2F%2Fm.facebook.com%2F" + "home.php'%7D%7D)()")
                else
                    webView.loadUrl("$MBASIC_FULL_URL/menu/bookmarks/?ref_component=mbasic_home_header&ref_page=%2Fwap%2Fhome.php&refid=8")
                setTitle(R.string.menu_mainmenu)
                item.isChecked = true
            }
            R.id.nav_events -> {
                webView.visibility = View.INVISIBLE

                webView.loadUrl("$baseUrl/events/")
                css.append("#page{top:0}")
                item.isChecked = true
            }
            R.id.nav_photos -> {
                webView.visibility = View.INVISIBLE

                webView.loadUrl("$baseUrl/photos/")
            }
            R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            R.id.nav_back -> if (webView.canGoBack())
                webView.goBack()
            R.id.nav_exitapp -> finishAffinity()
        }
        drawer.closeDrawers()
        return true
    }

    @JavascriptInterface
    fun LoadVideo(video_url: String) {
        val video = Intent(this, Video::class.java)
        video.putExtra(VIDEO_URL, video_url)
        startActivity(video)
    }

    private fun setupNavigationView() {
        navigationView.setNavigationItemSelectedListener(this)
        // Hide buttons if they are disabled
        if (!preferencesService.isMenuVisible("nav_groups"))
            navigationView.menu.findItem(R.id.nav_groups).isVisible = false
        if (!preferencesService.isMenuVisible("nav_mainmenu"))
            navigationView.menu.findItem(R.id.nav_mainmenu).isVisible = false
        if (!preferencesService.isMenuVisible("nav_most_recent"))
            navigationView.menu.findItem(R.id.nav_most_recent).isVisible = false
        if (!preferencesService.isMenuVisible("nav_events"))
            navigationView.menu.findItem(R.id.nav_events).isVisible = false
        if (!preferencesService.isMenuVisible("nav_photos"))
            navigationView.menu.findItem(R.id.nav_photos).isVisible = false
        if (!preferencesService.isMenuVisible("nav_back"))
            navigationView.menu.findItem(R.id.nav_back).isVisible = false
        if (!preferencesService.isMenuVisible("nav_exitapp"))
            navigationView.menu.findItem(R.id.nav_exitapp).isVisible = false
        if (!preferencesService.isMenuVisible("nav_top_stories"))
            navigationView.menu.findItem(R.id.nav_top_stories).isVisible = false
        if (!preferencesService.isMenuVisible("nav_friendreq"))
            navigationView.menu.findItem(R.id.nav_friendreq).isVisible = false
    }

    private fun setupSwipeView() {
        // Start the Swipe to reload listener
        swipeView.setColorSchemeResources(android.R.color.white)
        swipeView.setProgressBackgroundColorSchemeColor(Theme.getColor(this))
        swipeView.setOnRefreshListener { webView.reload() }
    }

    private fun setupWebView() {
        val mostRecentTv = navigationView.menu.findItem(R.id.nav_most_recent).actionView as TextView
        val friendsRegTv = navigationView.menu.findItem(R.id.nav_friendreq).actionView as TextView

        webView.setUpOnScrollChanged(floatingActionMenu, application.resources.getDimensionPixelOffset(R.dimen.fab_scroll_threshold))

        webView.updateSettings()
        webView.addJavascriptInterface(JavaScriptInterfaces(this, mostRecentTv, friendsRegTv), "android")
        webView.addJavascriptInterface(this, "Vid")

        webView.webViewClient = WebViewClient(this, baseUrl)
        webView.webChromeClient = WebChromeClient(this)
    }

    private fun setupColor() {
        // Setup navigation and status bar color
        window.navigationBarColor = applicationContext.getColor(R.color.MFBPrimaryDark)
        window.statusBarColor = applicationContext.getColor(R.color.MFBPrimary)
    }

    private fun setupFabListener() {
        val fabOnClickListener = FabOnClickListener(this, baseUrl)
        findViewById<View>(R.id.statusFab).setOnClickListener(fabOnClickListener)
        findViewById<View>(R.id.photoFab).setOnClickListener(fabOnClickListener)
        findViewById<View>(R.id.checkinFab).setOnClickListener(fabOnClickListener)
        findViewById<View>(R.id.topFab).setOnClickListener(fabOnClickListener)
        findViewById<View>(R.id.shareFab).setOnClickListener(fabOnClickListener)
    }

    private fun loadWebViewUrl() {
        when (preferencesService.getStartUrl()) {
            "Most_recent" -> webView.loadUrl("$baseUrl/home.php?sk=h_chr")
            "Top_stories" -> webView.loadUrl("$baseUrl/home.php?sk=h_nor")
            "Messages" -> webView.loadUrl("$baseUrl/messages/")
        }
    }

    private fun setupProfileImage() {
        // Add OnClick listener to Profile picture
        val profileImage = navigationView.getHeaderView(0).findViewById<ImageView>(R.id.profile_picture)
        profileImage.isClickable = true
        profileImage.setOnClickListener { _ ->
            drawer.closeDrawers()
            webView.loadUrl("$baseUrl/me")
        }
    }

    private fun setupBaseUrl() {
        baseUrl = if (!preferencesService.shouldSaveData())
            MOBILE_FULL_URL
        else
            MBASIC_FULL_URL
    }

    private fun urlIntent(@NonNull intent: Intent) {
        if (Intent.ACTION_SEND == intent.action && intent.type != null) {
            if (URLUtil.isValidUrl(intent.getStringExtra(Intent.EXTRA_TEXT))) {
                try {
                    webView.loadUrl("$MBASIC_FULL_URL/composer/?text=" + URLEncoder.encode(intent.getStringExtra(Intent.EXTRA_TEXT), "utf-8"))
                } catch (uee: UnsupportedEncodingException) {
                    Log.e(TAG, uee.message, uee)
                }
            }
        }

        if (intent.extras != null)
            urlIntent = intent.extras!!.getString(JOB_URL)

        if (intent.dataString != null) {
            urlIntent = getIntent().dataString
            if (intent.dataString!!.contains("profile"))
                urlIntent = urlIntent!!.replace("fb://profile/", "https://facebook.com/")
        }

        if (Intent.ACTION_SEND == intent.action && intent.type != null) {
            if (intent.type!!.startsWith("image/") || intent.type!!.startsWith("video/") || intent.type!!.startsWith("audio/")) {
                sharedFromGallery = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                css.append("#mbasic_inline_feed_composer{display:initial}")
                webView.loadUrl(MOBILE_FULL_URL)
            }
        }

        val newUrl = urlIntent
        val moreNewUrl: String
        if (newUrl != null && newUrl.contains(DESKTOP_URL)) {
            moreNewUrl = newUrl.replace(DESKTOP_URL, MOBILE_URL)
            webView.loadUrl(moreNewUrl)
        } else if (newUrl != null && newUrl.contains(WEB_URL)) {
            moreNewUrl = newUrl.replace(WEB_URL, MOBILE_URL)
            webView.loadUrl(moreNewUrl)
        } else {
            webView.loadUrl(urlIntent)
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}
