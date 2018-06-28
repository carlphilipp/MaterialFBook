package me.zeeroooo.materialfb.webview

import android.webkit.JavascriptInterface

import me.zeeroooo.materialfb.activity.MainActivity

class JavaScriptInterfaces(private val activity: MainActivity) {

    @JavascriptInterface
    fun getNums(notifications: String, messages: String, requests: String, feed: String) {
        val requestsInt = if (Helpers.isInteger(requests)) requests.toInt() else 0
        val mrInt = if (Helpers.isInteger(feed)) feed.toInt() else 0
        activity.runOnUiThread {
            activity.setRequestsNum(requestsInt)
            activity.setMrNum(mrInt)
        }
    }
}
