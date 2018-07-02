package me.zeeroooo.materialfb.service

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

object PreferenceService {

    fun getTheme(context: Context): String {
        return getPreferences(context).getString("app_theme", "MaterialFBook")
    }

    private fun getPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
}