package me.zeeroooo.materialfb.webview

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.UrlQuerySanitizer
import android.os.AsyncTask
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import me.zeeroooo.materialfb.R
import me.zeeroooo.materialfb.activity.App
import me.zeeroooo.materialfb.activity.MainActivity
import me.zeeroooo.materialfb.activity.Photo
import me.zeeroooo.materialfb.activity.Video
import me.zeeroooo.materialfb.misc.Constant.Preference.VIDEO_URL
import me.zeeroooo.materialfb.misc.Constant.REQUEST_AUTHORIZE_CODE
import me.zeeroooo.materialfb.misc.Constant.Url.DOMAIN
import me.zeeroooo.materialfb.misc.Constant.Url.MBASIC_FULL_URL
import me.zeeroooo.materialfb.misc.Constant.Url.MOBILE_FULL_URL
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.io.IOException

class WebViewClient(private val activity: MainActivity, private val baseUrl: String) : android.webkit.WebViewClient() {

    private val preferencesService = App.instance.preferenceService
    private var elements: Elements? = null

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        var url = request.url.toString()
        // clean an url from facebook redirection before processing (no more blank pages on back)
        url = Helpers.cleanAndDecodeUrl(url)

        if (url.contains("mailto:")) {
            val mailto = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            activity.startActivity(mailto)
        }

        if (Uri.parse(url).host.endsWith("facebook.com")
                || Uri.parse(url).host.endsWith("*.facebook.com")
                || Uri.parse(url).host.endsWith("akamaihd.net")
                || Uri.parse(url).host.endsWith("ad.doubleclick.net")
                || Uri.parse(url).host.endsWith("sync.liverail.com")
                || Uri.parse(url).host.endsWith("cdn.fbsbx.com")
                || Uri.parse(url).host.endsWith("lookaside.fbsbx.com")) {
            return false
        }

        if (isVideoUrl(url)) {
            val video = Intent(activity, Video::class.java)
            video.putExtra(VIDEO_URL, url)
            activity.startActivity(video)
            return true
        }

        if (url.contains("giphy") || url.contains("gifspace") || url.contains("tumblr") || url.contains("gph.is") || url.contains("gif") || url.contains("fbcdn.net") || url.contains("imgur")) {
            if (url.contains("giphy") || url.contains("gph")) {
                if (!url.endsWith(".gif")) {
                    if (url.contains("giphy.com") || url.contains("html5"))
                        url = String.format("http://media.giphy.com/media/%s/giphy.gif", url.replace("http://giphy.com/gifs/", ""))
                    else if (url.contains("gph.is") && !url.contains("html5")) {
                        view.loadUrl(url)
                        url = String.format("http://media.giphy.com/media/%s/giphy.gif", url.replace("http://giphy.com/gifs/", ""))
                    }

                    if (url.contains("media.giphy.com/media/") && !url.contains("html5")) {
                        val giphy = url.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val giphyId = giphy[giphy.size - 1]
                        url = "http://media.giphy.com/media/$giphyId"
                    }
                    if (url.contains("media.giphy.com/media/http://media")) {
                        val gph = url.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val gphId = gph[gph.size - 2]
                        url = "http://media.giphy.com/media/$gphId/giphy.gif"
                    }
                    if (url.contains("html5/giphy.gif")) {
                        val giphyHtml5 = url.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val giphyHtml5Id = giphyHtml5[giphyHtml5.size - 3]
                        url = "http://media.giphy.com/media/$giphyHtml5Id/giphy.gif"
                    }
                }
                if (url.contains("?")) {
                    val giphy1 = url.split("\\?".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val giphyHtml5Id = giphy1[0]
                    url = "$giphyHtml5Id/giphy.gif"
                }
            }

            if (url.contains("gifspace")) {
                if (!url.endsWith(".gif"))
                    url = String.format("http://gifspace.net/image/%s.gif", url.replace("http://gifspace.net/image/", ""))
            }

            if (url.contains("phygee")) {
                if (!url.endsWith(".gif")) {
                    getSrc(url, "span", "img")
                    url = "http://www.phygee.com/" + elements?.attr("src")
                }
            }

            if (url.contains("imgur")) {
                if (!url.endsWith(".gif") && !url.endsWith(".jpg")) {
                    getSrc(url, "div.post-image", "img")
                    url = "https:" + elements?.attr("src")
                }
            }

            if (url.contains("media.upgifs.com")) {
                if (!url.endsWith(".gif")) {
                    getSrc(url, "div.gif-pager-container", "img#main-gif")
                    url = elements!!.attr("src")
                }
            }

            imageLoader(url, view)
            return true
        } else {
            // Open external links in browser
            val browser = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            activity.startActivity(browser)
            return true
        }
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        activity.swipeView.isRefreshing = true
        if (url.contains("$MBASIC_FULL_URL/home.php?s=")) {
            view.loadUrl(baseUrl)
        }
    }

    override fun onLoadResource(view: WebView, url: String) {
        JavaScriptHelpers.videoView(view)
        if (activity.swipeView.isRefreshing)
            JavaScriptHelpers.loadCSS(view, activity.css.toString())
        if (url.contains("$DOMAIN/composer/mbasic/") || url.contains("$MOBILE_FULL_URL/sharer.php?sid="))
            activity.css.append("#page{top:0}")

        // Do not start Photo activity anymore when clicking on a photo in news feed
        //if (url.contains("/photos/viewer/"))
        //    imageLoader(activity.getBaseURL() + "photo/view_full_size/?fbid=" + url.substring(url.indexOf("photo=") + 6).split("&")[0], view);

        if (url.contains("/photo/view_full_size/?fbid="))
            imageLoader(url.split("&ref_component".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0], view)
    }

    override fun onPageFinished(view: WebView, url: String) {
        activity.swipeView.isRefreshing = false

        when (preferencesService.getWebTheme()) {
            "FacebookMobile" -> {
            }
            "Material" -> activity.css.append(activity.getString(R.string.Material))
            "MaterialAmoled" -> {
                activity.css.append(activity.getString(R.string.MaterialAmoled))
                activity.css.append("::selection {background: #D3D3D3;}")
            }
            "MaterialBlack" -> activity.css.append(activity.getString(R.string.MaterialBlack))
            "MaterialPink" -> activity.css.append(activity.getString(R.string.MaterialPink))
            "MaterialGrey" -> activity.css.append(activity.getString(R.string.MaterialGrey))
            "MaterialGreen" -> activity.css.append(activity.getString(R.string.MaterialGreen))
            "MaterialRed" -> activity.css.append(activity.getString(R.string.MaterialRed))
            "MaterialLime" -> activity.css.append(activity.getString(R.string.MaterialLime))
            "MaterialYellow" -> activity.css.append(activity.getString(R.string.MaterialYellow))
            "MaterialPurple" -> activity.css.append(activity.getString(R.string.MaterialPurple))
            "MaterialLightBlue" -> activity.css.append(activity.getString(R.string.MaterialLightBlue))
            "MaterialOrange" -> activity.css.append(activity.getString(R.string.MaterialOrange))
            "MaterialGooglePlayGreen" -> activity.css.append(activity.getString(R.string.MaterialGPG))
        }

        if (url.contains("lookaside") || url.contains("cdn.fbsbx.com")) {
            activity.url = url
            requestStoragePermission()
        }

        // Enable or disable FAB
        activity.floatingActionMenu.visibility =
                if (url.contains("messages") || !preferencesService.getBoolean("fab_enable"))
                    View.GONE
                else
                    View.VISIBLE

        if (url.contains("$MBASIC_FULL_URL/composer/?text=")) {
            val sanitizer = UrlQuerySanitizer()
            sanitizer.allowUnregisteredParamaters = true
            sanitizer.parseUrl(url)
            val param = sanitizer.getValue("text")
            view.loadUrl("javascript:(function(){document.querySelector('#composerInput').innerHTML='$param'})()")
        }

        if (url.contains("$MOBILE_FULL_URL/public/")) {
            val user = url.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val profile = user[user.size - 1]
            view.loadUrl("javascript:(function(){document.querySelector('input#u_0_0._5whq.input').value='$profile'})()")
            view.loadUrl("javascript:(function(){try{document.querySelector('button#u_0_1.btn.btnD.mfss.touchable').disabled = false}catch(_){}})()")
            view.loadUrl("javascript:(function(){try{document.querySelector('button#u_0_1.btn.btnD.mfss.touchable').click()}catch(_){}})()")
        }

        if (preferencesService.getBoolean("hide_menu_bar"))
            activity.css.append("#page{top:-45px}")
        // Hide the status editor on the News Feed if setting is enabled
        if (preferencesService.getBoolean("hide_editor_newsfeed", true))
            activity.css.append("#mbasic_inline_feed_composer{display:none}")

        // Hide the top story panel in news feed right before the status box
        if (preferencesService.getBoolean("hide_top_story", true))
            //activity.css.plus("._59e9._55wr._4g33._400s{display:none}")
            activity.css = activity.css.append("._59e9._55wr._4g33._400s{display:none}")

        // Hide 'Sponsored' content (ads)
        if (preferencesService.getBoolean("hide_sponsored", true))
            activity.css.append("article[data-ft*=ei]{display:none}")

        // Hide birthday content from News Feed
        if (preferencesService.getBoolean("hide_birthdays", true))
            activity.css.append("article#u_1j_4{display:none}article._55wm._5e4e._5fjt{display:none}")

        if (preferencesService.getBoolean("comments_recently", true))
            activity.css.append("._15ks+._4u3j{display:none}")

        if (activity.sharedFromGallery != null)
            view.loadUrl("javascript:(function(){try{document.getElementsByClassName(\"_56bz _54k8 _52jh _5j35 _157e\")[0].click()}catch(_){document.getElementsByClassName(\"_50ux\")[0].click()}})()")

        activity.css.append("article#u_0_q._d2r{display:none}*{-webkit-tap-highlight-color:transparent;outline:0}")
        activity.webView.visibility = View.VISIBLE
    }

    private fun getSrc(url: String, select: String, select2: String) {
        object : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(params: Array<Void>): Void? {
                try {
                    val document = Jsoup.connect(url).get()
                    elements = document.select(select).select(select2)
                } catch (e: IOException) {
                    Log.e(TAG, e.message, e)
                }
                return null
            }
        }.execute()
    }

    private fun imageLoader(url: String, view: WebView) {
        activity.startActivity(Intent(activity, Photo::class.java).putExtra("link", url).putExtra("title", view.title))
        view.stopLoading()
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_AUTHORIZE_CODE)
    }

    private fun isVideoUrl(url: String): Boolean {
        return url.contains(".mp4") || url.contains("video-ort2-1.xx.fbcdn.net") || url.contains("video.ford4-1.fna.fbcdn.net")
    }

    companion object {
        private val TAG = WebViewClient::class.java.simpleName
    }
}
