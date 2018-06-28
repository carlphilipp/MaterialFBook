/**
 * Code taken from FaceSlim by indywidualny. Thanks.
 */
package me.zeeroooo.materialfb.fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.LocaleList
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import me.zeeroooo.materialfb.R
import me.zeeroooo.materialfb.activity.More
import me.zeeroooo.materialfb.notification.Scheduler
import me.zeeroooo.materialfb.ui.CookingAToast
import java.util.Locale

class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {

    private lateinit var currentContext: Context
    private lateinit var preferences: SharedPreferences
    private lateinit var scheduler: Scheduler

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.currentContext = context
        scheduler = Scheduler(context)
    }

    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.settings)
        preferences = PreferenceManager.getDefaultSharedPreferences(activity)

        findPreference("notifications_settings").onPreferenceClickListener = this
        findPreference("navigation_menu_settings").onPreferenceClickListener = this
        findPreference("moreandcredits").onPreferenceClickListener = this
        findPreference("location_enabled").onPreferenceClickListener = this
        findPreference("save_data").onPreferenceClickListener = this
        findPreference("notif").onPreferenceClickListener = this
        findPreference("localeSwitcher").setOnPreferenceChangeListener { _, o ->
            val locale = Locale(o.toString())
            Locale.setDefault(locale)
            val config = Configuration()
            val localeList = LocaleList(locale)
            config.locales = localeList
            currentContext.createConfigurationContext(config)
            true
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            "notifications_settings" -> {
                fragmentManager!!.beginTransaction().addToBackStack(null).replace(R.id.content_frame, NotificationsSettingsFragment()).commit()
                return true
            }
            "navigation_menu_settings" -> {
                fragmentManager!!.beginTransaction().addToBackStack(null).replace(R.id.content_frame, NavigationMenuFragment()).commit()
                return true
            }
            "moreandcredits" -> {
                startActivity(Intent(activity, More::class.java))
                return true
            }
            "location_enabled" -> {
                ActivityCompat.requestPermissions(activity!!, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
                return true
            }
            "save_data" -> {
                setScheduler()
                return true
            }
            "notif" -> {
                setScheduler()
                return true
            }
        }
        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED))
                CookingAToast.cooking(activity!!, getString(R.string.permission_denied), Color.WHITE, Color.parseColor("#ff4444"), R.drawable.ic_error, true).show()
        }
    }

    private fun setScheduler() {
        if (preferences.getBoolean("notif", false) && !preferences.getBoolean("save_data", false)) {
            scheduler.schedule(Integer.parseInt(preferences.getString("notif_interval", "60000")), true)
        } else {
            scheduler.cancel()
        }
    }
}