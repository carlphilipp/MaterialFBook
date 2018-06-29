package me.zeeroooo.materialfb.listener

import android.content.Intent
import android.view.View

import me.zeeroooo.materialfb.R
import me.zeeroooo.materialfb.activity.MainActivity

class FabOnClickListener(private val activity: MainActivity) : View.OnClickListener {

    override fun onClick(view: View) {
        val baseUrl = activity.baseURL
        when (view.id) {
            R.id.checkinFab -> {
                activity.webView.loadUrl(clickOn("u_0_11", baseUrl))
                activity.swipeView.isEnabled = false
            }
            R.id.photoFab -> {
                activity.webView.loadUrl(clickOn("u_0_10", baseUrl))
                activity.swipeView.isEnabled = false
            }
            R.id.statusFab -> {
                activity.webView.loadUrl("javascript:(function(){try{document.querySelector('button[name=\"view_overview\"]').click()}catch(_){window.location.href=\"$baseUrl?pageload=composer\"}})()")
                activity.swipeView.isEnabled = false
            }
            R.id.shareFab -> {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_TEXT, activity.webView.url)
                activity.startActivity(Intent.createChooser(shareIntent, activity.getString(R.string.context_share_link)))
            }
            R.id.topFab -> activity.webView.scrollTo(0, 0)
        }
        activity.floatingActionMenu.close(true)
    }

    private fun clickOn(id: String, baseUrl: String): String {
        return "javascript:(function(){try{document.querySelector('div[id=\"$id\"]').click()}catch(_){window.location.href=\"$baseUrl?pageload=composer_photo\"}})()"
    }
}
