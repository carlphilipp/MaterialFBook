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
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.preference.PreferenceManager
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.widget.ImageView
import android.widget.TextView
import com.github.clans.fab.FloatingActionMenu
import me.zeeroooo.materialfb.R
import me.zeeroooo.materialfb.listener.FabOnClickListener
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

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit var preferences: SharedPreferences
        private set
    lateinit var webView: MFBWebView
        private set
    private lateinit var drawer: DrawerLayout
    lateinit var swipeView: SwipeRefreshLayout
        private set
    lateinit var floatingActionMenu: FloatingActionMenu
        private set
    private lateinit var navigationView: NavigationView
    private lateinit var mostRecentTv: TextView
    private lateinit var friendsRegTv: TextView
    lateinit var profileNameTv: TextView
        private set
    lateinit var coverIv: ImageView
        private set
    lateinit var profilePictureIv: ImageView
        private set
    private lateinit var downloadManager: DownloadManager

    lateinit var baseURL: String
        private set
    var cameraPhotoPath: String? = null
    val css = StringBuilder()
    var url: String? = null
    private var urlIntent: String? = null
    var filePathCallback: ValueCallback<Array<Uri>>? = null
    var sharedFromGallery: Uri? = null
        private set
    private var badgeUpdateHandler: Handler? = null
    private var badgeTask: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        Theme.Temas(this, preferences)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        drawer = findViewById(R.id.drawer_layout)
        swipeView = findViewById(R.id.swipeLayout)
        floatingActionMenu = findViewById(R.id.menuFAB)
        navigationView = findViewById(R.id.nav_view)
        mostRecentTv = navigationView.menu.findItem(R.id.nav_most_recent).actionView as TextView
        friendsRegTv = navigationView.menu.findItem(R.id.nav_friendreq).actionView as TextView

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
        if (intent.getBooleanExtra("apply", false)) {
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

        if (Helpers.cookie != null && !preferences.getBoolean("save_data", false)) {
            badgeUpdateHandler = Handler()
            badgeTask = Runnable {
                JavaScriptHelpers.updateNumsService(webView)
                badgeUpdateHandler!!.postDelayed(badgeTask, 15000)
            }
            badgeTask!!.run()
            UserInfoAsyncTask(this).execute()
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
        if (badgeTask != null && badgeUpdateHandler != null)
            badgeUpdateHandler!!.removeCallbacks(badgeTask)
        if (preferences.getBoolean("clear_cache", false))
            Utils.deleteCache(this)
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

        filePathCallback!!.onReceiveValue(results)
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

                webView.loadUrl(baseURL + "home.php?sk=h_nor")
                setTitle(R.string.menu_top_stories)
                item.isChecked = true
            }
            R.id.nav_most_recent -> {
                webView.visibility = View.INVISIBLE

                webView.loadUrl(baseURL + "home.php?sk=h_chr'")
                setTitle(R.string.menu_most_recent)
                item.isChecked = true
                Helpers.uncheckRadioMenu(navigationView.menu)
            }
            R.id.nav_friendreq -> {
                webView.visibility = View.INVISIBLE

                webView.loadUrl(baseURL + "friends/center/requests/")
                setTitle(R.string.menu_friendreq)
                item.isChecked = true
            }
            R.id.nav_groups -> {
                webView.visibility = View.INVISIBLE

                webView.loadUrl(baseURL + "groups/?category=membership")
                css.append("._129- {position:initial}")
                item.isChecked = true
            }
            R.id.nav_mainmenu -> {
                webView.visibility = View.INVISIBLE

                if (!preferences.getBoolean("save_data", false))
                    webView.loadUrl("javascript:(function()%7Btry%7Bdocument.querySelector('%23bookmarks_jewel%20%3E%20a').click()%7Dcatch(_)%7Bwindow.location.href%3D'" + "https%3A%2F%2Fm.facebook.com%2F" + "home.php'%7D%7D)()")
                else
                    webView.loadUrl("https://mbasic.facebook.com/menu/bookmarks/?ref_component=mbasic_home_header&ref_page=%2Fwap%2Fhome.php&refid=8")
                setTitle(R.string.menu_mainmenu)
                item.isChecked = true
            }
            R.id.nav_events -> {
                webView.visibility = View.INVISIBLE

                webView.loadUrl(baseURL + "events/")
                css.append("#page{top:0}")
                item.isChecked = true
            }
            R.id.nav_photos -> {
                webView.visibility = View.INVISIBLE

                webView.loadUrl(baseURL + "photos/")
            }
            R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            R.id.nav_back -> if (webView.canGoBack())
                webView.goBack()
            R.id.nav_exitapp -> finishAffinity()
            else -> {
            }
        }
        drawer.closeDrawers()
        return true
    }

    @JavascriptInterface
    fun LoadVideo(video_url: String) {
        val video = Intent(this, Video::class.java)
        video.putExtra("video_url", video_url)
        startActivity(video)
    }

    fun setRequestsNum(num: Int) {
        txtFormat(friendsRegTv, num, Color.RED)
    }

    fun setMrNum(num: Int) {
        txtFormat(mostRecentTv, num, Color.RED)
    }

    private fun setupNavigationView() {
        navigationView.setNavigationItemSelectedListener(this)
        // Hide buttons if they are disabled
        if (!preferences.getBoolean("nav_groups", false))
            navigationView.menu.findItem(R.id.nav_groups).isVisible = false
        if (!preferences.getBoolean("nav_mainmenu", false))
            navigationView.menu.findItem(R.id.nav_mainmenu).isVisible = false
        if (!preferences.getBoolean("nav_most_recent", false))
            navigationView.menu.findItem(R.id.nav_most_recent).isVisible = false
        if (!preferences.getBoolean("nav_events", false))
            navigationView.menu.findItem(R.id.nav_events).isVisible = false
        if (!preferences.getBoolean("nav_photos", false))
            navigationView.menu.findItem(R.id.nav_photos).isVisible = false
        if (!preferences.getBoolean("nav_back", false))
            navigationView.menu.findItem(R.id.nav_back).isVisible = false
        if (!preferences.getBoolean("nav_exitapp", false))
            navigationView.menu.findItem(R.id.nav_exitapp).isVisible = false
        if (!preferences.getBoolean("nav_top_stories", false))
            navigationView.menu.findItem(R.id.nav_top_stories).isVisible = false
        if (!preferences.getBoolean("nav_friendreq", false))
            navigationView.menu.findItem(R.id.nav_friendreq).isVisible = false
    }

    private fun setupSwipeView() {
        // Start the Swipe to reload listener
        swipeView.setColorSchemeResources(android.R.color.white)
        swipeView.setProgressBackgroundColorSchemeColor(Theme.getColor(this))
        swipeView.setOnRefreshListener { webView.reload() }
    }

    private fun setupWebView() {
        webView.setUpOnScrollChanged(floatingActionMenu, application.resources.getDimensionPixelOffset(R.dimen.fab_scroll_threshold))

        webView.updateSettings(preferences)
        webView.addJavascriptInterface(JavaScriptInterfaces(this), "android")
        webView.addJavascriptInterface(this, "Vid")

        webView.webViewClient = WebViewClient(this)
        webView.webChromeClient = WebChromeClient(this)
    }

    private fun setupColor() {
        // Setup navigation and status bar color
        window.navigationBarColor = applicationContext.getColor(R.color.MFBPrimaryDark)
        window.statusBarColor = applicationContext.getColor(R.color.MFBPrimary)
    }

    private fun setupFabListener() {
        val fabOnClickListener = FabOnClickListener(this)
        findViewById<View>(R.id.textFAB).setOnClickListener(fabOnClickListener)
        findViewById<View>(R.id.photoFAB).setOnClickListener(fabOnClickListener)
        findViewById<View>(R.id.checkinFAB).setOnClickListener(fabOnClickListener)
        findViewById<View>(R.id.topFAB).setOnClickListener(fabOnClickListener)
        findViewById<View>(R.id.shareFAB).setOnClickListener(fabOnClickListener)
    }

    private fun loadWebViewUrl() {
        when (preferences.getString("start_url", "Most_recent")) {
            "Most_recent" -> webView.loadUrl(baseURL + "home.php?sk=h_chr")
            "Top_stories" -> webView.loadUrl(baseURL + "home.php?sk=h_nor")
            "Messages" -> webView.loadUrl(baseURL + "messages/")
            else -> {
            }
        }
    }

    private fun setupProfileImage() {
        // Add OnClick listener to Profile picture
        val profileImage = navigationView.getHeaderView(0).findViewById<ImageView>(R.id.profile_picture)
        profileImage.isClickable = true
        profileImage.setOnClickListener { _ ->
            drawer.closeDrawers()
            webView.loadUrl(baseURL + "me")
        }
    }

    private fun txtFormat(t: TextView, i: Int, color: Int) {
        t.text = String.format("%s", i)
        t.setTextColor(color)
        t.gravity = Gravity.CENTER_VERTICAL
        t.setTypeface(null, Typeface.BOLD)
        t.visibility = if (i > 0) View.VISIBLE else View.INVISIBLE
    }

    private fun setupBaseUrl() {
        baseURL = if (!preferences.getBoolean("save_data", false))
            "https://m.facebook.com/"
        else
            "https://mbasic.facebook.com/"
    }

    private fun urlIntent(intent: Intent) {
        if (Intent.ACTION_SEND == intent.action && intent.type != null) {
            if (URLUtil.isValidUrl(intent.getStringExtra(Intent.EXTRA_TEXT))) {
                try {
                    webView.loadUrl("https://mbasic.facebook.com/composer/?text=" + URLEncoder.encode(intent.getStringExtra(Intent.EXTRA_TEXT), "utf-8"))
                } catch (uee: UnsupportedEncodingException) {
                    Log.e(TAG, uee.message, uee)
                }

            }
        }

        if (intent.extras != null)
            urlIntent = intent.extras!!.getString("Job_url")

        if (intent.dataString != null) {
            urlIntent = getIntent().dataString
            if (intent.dataString!!.contains("profile"))
                urlIntent = urlIntent!!.replace("fb://profile/", "https://facebook.com/")
        }

        if (Intent.ACTION_SEND == intent.action && intent.type != null) {
            if (intent.type!!.startsWith("image/") || intent.type!!.startsWith("video/") || intent.type!!.startsWith("audio/")) {
                sharedFromGallery = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                css.append("#mbasic_inline_feed_composer{display:initial}")
                webView.loadUrl("https://m.facebook.com")
            }
        }

        val newUrl = urlIntent
        val moreNewUrl: String
        if (newUrl != null && newUrl.contains("www.facebook.com")) {
            moreNewUrl = newUrl.replace("www.facebook.com", "m.facebook.com")
            webView.loadUrl(moreNewUrl)
        } else if (newUrl != null && newUrl.contains("web.facebook.com")) {
            moreNewUrl = newUrl.replace("web.facebook.com", "m.facebook.com")
            webView.loadUrl(moreNewUrl)
        } else {
            webView.loadUrl(urlIntent)
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        const val INPUT_FILE_REQUEST_CODE = 1
    }
}
