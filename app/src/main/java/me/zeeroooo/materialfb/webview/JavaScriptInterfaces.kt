package me.zeeroooo.materialfb.webview

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.webkit.JavascriptInterface
import android.widget.TextView
import me.zeeroooo.materialfb.activity.MainActivity

class JavaScriptInterfaces(private val activity: MainActivity, private val mostRecentTv: TextView, private val friendsRegTv: TextView) {

    @JavascriptInterface
    fun getNums(notifications: String, messages: String, requests: String, feed: String) {
        val requestsInt = if (Helpers.isInteger(requests)) requests.toInt() else 0
        val mostRecentInt = if (Helpers.isInteger(feed)) feed.toInt() else 0
        activity.runOnUiThread {
            setRequestsNum(requestsInt)
            setMostRecent(mostRecentInt)
        }
    }

    private fun setRequestsNum(num: Int) {
        txtFormat(friendsRegTv, num, Color.RED)
    }

    private fun setMostRecent(num: Int) {
        txtFormat(mostRecentTv, num, Color.RED)
    }

    private fun txtFormat(textView: TextView, i: Int, color: Int) {
        textView.text = String.format("%s", i)
        textView.gravity = Gravity.CENTER_VERTICAL
        textView.visibility = if (i > 0) View.VISIBLE else View.INVISIBLE
        textView.setTextColor(color)
        textView.setTypeface(null, Typeface.BOLD)
    }
}
