package me.zeeroooo.materialfb.misc

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.util.Log
import android.webkit.CookieManager
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import me.zeeroooo.materialfb.activity.MainActivity
import me.zeeroooo.materialfb.misc.Constant.Url.DESKTOP_ME_FULL_URL
import me.zeeroooo.materialfb.misc.Constant.Url.MOBILE_FULL_URL
import me.zeeroooo.materialfb.webview.Helpers
import org.jsoup.Jsoup
import java.io.InputStream
import java.lang.ref.WeakReference
import java.net.URL

class UserInfoAsyncTask(activity: MainActivity,
                        profileNameTv: TextView,
                        coverIv: ImageView,
                        profilePictureIv: ImageView) : AsyncTask<Void, Void, Boolean>() {

    private val weakActivity = WeakReference(activity)
    private val weakProfileName = WeakReference(profileNameTv)
    private val weakCover = WeakReference(coverIv)
    private val weakProfilePicture = WeakReference(profilePictureIv)
    private var name: String? = null
    private var coverBitmap: Bitmap? = null
    private var profileBitmap: Bitmap? = null

    override fun doInBackground(params: Array<Void>): Boolean {
        try {
            val element = Jsoup.connect(DESKTOP_ME_FULL_URL)
                    .cookie(MOBILE_FULL_URL, CookieManager.getInstance().getCookie(MOBILE_FULL_URL))
                    .timeout(300000)
                    .get()
                    .body()
            name = element.select("input[name=q]").attr("value")

            val content = element.toString()
            val coverUrl = extractUrl(content, "<img class=\"coverPhotoImg photo img\" src=\"".toRegex())
            coverBitmap = BitmapFactory.decodeStream(URL(coverUrl).content as InputStream)

            val profileUrl = extractUrl(content, "<img class=\"_11kf img\" alt=\"[a-zA-Z,:0-9 ]+\" src=\"".toRegex())
            profileBitmap = BitmapFactory.decodeStream(URL(profileUrl).content as InputStream)
            return true
        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
        }
        return false
    }

    override fun onPostExecute(shouldProcess: Boolean) {
        val activity = weakActivity.get()
        val profileNameTv = weakProfileName.get()
        val coverIv = weakCover.get()
        val profilePictureIv = weakProfilePicture.get()
        if (shouldProcess && activity != null && profileNameTv != null && coverIv != null && profilePictureIv != null) {
            try {
                profileNameTv.text = name
                Glide.with(activity)
                        .load(coverBitmap)
                        .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                        .into(coverIv)
                Glide.with(activity)
                        .load(profileBitmap)
                        .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL).circleCrop())
                        .into(profilePictureIv)
            } catch (e: Exception) {
                Log.e(TAG, e.message, e)
            }
        }
    }

    private fun extractUrl(content: String, regex: Regex): String {
        return Helpers.decodeImg(
                content.split(regex)
                        .dropLastWhile { it.isEmpty() }
                        .toTypedArray()[1]
                        .split("\"".toRegex())
                        .dropLastWhile { it.isEmpty() }
                        .toTypedArray()[0])
    }

    companion object {
        private val TAG = UserInfoAsyncTask::class.java.simpleName
    }
}