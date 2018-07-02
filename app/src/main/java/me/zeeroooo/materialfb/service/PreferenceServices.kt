package me.zeeroooo.materialfb.service

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import me.zeeroooo.materialfb.R

class PreferenceServices(private val context: Context) {

    private val saveData by lazy { context.getString(R.string.pref_save_data) }
    private val prefStartUrl by lazy { context.getString(R.string.pref_start_url) }

    fun getStartUrl(): String {
        return getPreferences().getString(prefStartUrl, "Most_recent")
    }

    fun getTextScale(): Int {
        return getPreferences().getInt("textScale", 1)
    }

    fun getBoolean(str: String): Boolean {
        return getBoolean(str, false)
    }

    fun getBoolean(str: String, boolean: Boolean): Boolean {
        return getPreferences().getBoolean(str, boolean)
    }

    fun getAppTheme(): String {
        return getPreferences().getString("app_theme", "MaterialFBook")
    }

    fun getWebTheme(): String {
        return getPreferences().getString("web_themes", "FacebookMobile")
    }

    fun isMenuVisible(menuName: String): Boolean {
        return getPreferences().getBoolean(menuName, false)
    }

    fun shouldClearCache(): Boolean {
        return getPreferences().getBoolean("clear_cache", false)
    }

    fun shouldSaveData(): Boolean {
        return getPreferences().getBoolean(saveData, false)
    }

    private fun getPreferences(): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
}