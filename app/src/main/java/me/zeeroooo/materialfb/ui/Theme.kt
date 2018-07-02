/*
 * Created by ZeeRooo
 * https://github.com/ZeeRooo
 */
package me.zeeroooo.materialfb.ui

import android.content.Context
import android.util.TypedValue

import me.zeeroooo.materialfb.R
import me.zeeroooo.materialfb.service.PreferenceService

object Theme {

    private val preferenceService: PreferenceService = PreferenceService

    // Thanks to Naman Dwivedi
    fun getColor(context: Context): Int {
        val colorPrimary = R.attr.colorPrimary
        val outValue = TypedValue()
        context.theme.resolveAttribute(colorPrimary, outValue, true)
        return outValue.data
    }

    fun applyTheme(context: Context) {
        val theme = preferenceService.getTheme(context)
        when (theme) {
            "MaterialFBook" -> context.setTheme(R.style.MFB)
            "Amoled" -> context.setTheme(R.style.Black)
            "Black" -> context.setTheme(R.style.Black)
            "Pink" -> context.setTheme(R.style.Pink)
            "Grey" -> context.setTheme(R.style.Grey)
            "Green" -> context.setTheme(R.style.Green)
            "Red" -> context.setTheme(R.style.Red)
            "Lime" -> context.setTheme(R.style.Lime)
            "Yellow" -> context.setTheme(R.style.Yellow)
            "Purple" -> context.setTheme(R.style.Purple)
            "LightBlue" -> context.setTheme(R.style.LightBlue)
            "Orange" -> context.setTheme(R.style.Orange)
            "GooglePlayGreen" -> context.setTheme(R.style.GooglePlayGreen)
        }
    }
}
