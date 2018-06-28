package me.zeeroooo.materialfb.webview

import android.view.Menu
import android.webkit.CookieManager
import me.zeeroooo.materialfb.misc.Constant.Url.MOBILE_FULL_URL

object Helpers {

    // Method to retrieve a single cookie
    // Return null as we found no cookie
    val cookie: String?
        get() {
            val cookieManager = CookieManager.getInstance()
            val cookies = cookieManager.getCookie("$MOBILE_FULL_URL/")
            if (cookies != null) {
                val temp = cookies.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (ar1 in temp) {
                    if (ar1.contains("c_user")) {
                        val temp1 = ar1.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        return temp1[1]
                    }
                }
            }
            return null
        }

    // Uncheck all items menu
    fun uncheckRadioMenu(menu: Menu) {
        for (i in 0 until menu.size()) {
            if (menu.getItem(i).isChecked) {
                menu.getItem(i).isChecked = false
                return
            }
        }
    }

    fun isInteger(str: String): Boolean {
        return str.matches("^-?\\d+$".toRegex())
    }

    // "clean" and decode an url, all in one
    fun cleanAndDecodeUrl(url: String): String {
        return decodeUrl(cleanUrl(url))
    }

    // "clean" an url and remove Facebook tracking redirection
    private fun cleanUrl(url: String): String {
        return url.replace("http://lm.facebook.com/l.php?u=", "")
                .replace("https://m.facebook.com/l.php?u=", "")
                .replace("http://0.facebook.com/l.php?u=", "")
                .replace("https://lm.facebook.com/l.php?u=", "")
                .replace("&h=.*".toRegex(), "")
                .replace("\\?acontext=.*".toRegex(), "")
                .replace("&SharedWith=".toRegex(), "")
    }

    // url decoder, recreate all the special characters
    private fun decodeUrl(url: String): String {
        return url.replace("%3C", "<")
                .replace("%3E", ">")
                .replace("%23", "#")
                .replace("%25", "%")
                .replace("%7B", "{")
                .replace("%7D", "}")
                .replace("%7C", "|")
                .replace("%5C", "\\")
                .replace("%5E", "^")
                .replace("%7E", "~")
                .replace("%5B", "[")
                .replace("%5D", "]")
                .replace("%60", "`")
                .replace("%3B", ";")
                .replace("%2F", "/")
                .replace("%3F", "?")
                .replace("%3A", ":")
                .replace("%40", "@")
                .replace("%3D", "=")
                .replace("%26", "&")
                .replace("%24", "$")
                .replace("%2B", "+")
                .replace("%22", "\"")
                .replace("%2C", ",")
                .replace("%20", " ")
    }

    fun decodeImg(imgUrl: String): String {
        return imgUrl.replace("\\3a ", ":")
                .replace("efg\\3d ", "oh=")
                .replace("\\3d ", "=")
                .replace("\\26 ", "&")
                .replace("\\", "")
                .replace("&amp;", "&")
    }
}
