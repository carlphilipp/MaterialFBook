/*
 * Created by ZeeRooo
 * https://github.com/ZeeRooo
 */
package me.zeeroooo.materialfb.ui

import android.content.Context
import android.content.SharedPreferences
import android.util.TypedValue

import me.zeeroooo.materialfb.R

object Theme {

    // Thanks to Naman Dwivedi
    fun getColor(context: Context): Int {
        val colorPrimary = R.attr.colorPrimary
        val outValue = TypedValue()
        context.theme.resolveAttribute(colorPrimary, outValue, true)
        return outValue.data
    }

    fun temas(ctxt: Context, preferences: SharedPreferences) {
        when (preferences.getString("app_theme", "MaterialFBook")) {
            "MaterialFBook" -> ctxt.setTheme(R.style.MFB)
            "Amoled" -> ctxt.setTheme(R.style.Black)
            "Black" -> ctxt.setTheme(R.style.Black)
            "Pink" -> ctxt.setTheme(R.style.Pink)
            "Grey" -> ctxt.setTheme(R.style.Grey)
            "Green" -> ctxt.setTheme(R.style.Green)
            "Red" -> ctxt.setTheme(R.style.Red)
            "Lime" -> ctxt.setTheme(R.style.Lime)
            "Yellow" -> ctxt.setTheme(R.style.Yellow)
            "Purple" -> ctxt.setTheme(R.style.Purple)
            "LightBlue" -> ctxt.setTheme(R.style.LightBlue)
            "Orange" -> ctxt.setTheme(R.style.Orange)
            "GooglePlayGreen" -> ctxt.setTheme(R.style.GooglePlayGreen)
        }
    }
}
