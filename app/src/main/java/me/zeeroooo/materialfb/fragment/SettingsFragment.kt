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

    private val prefSaveData by lazy { currentContext.getString(R.string.pref_save_data) }
    private val prefNotifSettings by lazy { currentContext.getString(R.string.pref_notifications_settings) }
    private val prefNavMenuSettings by lazy { currentContext.getString(R.string.pref_navigation_menu_settings) }
    private val prefMoreAndCredits by lazy { currentContext.getString(R.string.pref_more_and_credits) }
    private val prefLocationEnabled by lazy { currentContext.getString(R.string.pref_location_enabled) }
    private val prefNotif by lazy { currentContext.getString(R.string.pref_notif) }
    private val prefLocaleSwitcher by lazy { currentContext.getString(R.string.pref_locale_switcher) }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.currentContext = context
        scheduler = Scheduler(context)
    }

    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.settings)
        preferences = PreferenceManager.getDefaultSharedPreferences(activity)

        findPreference(prefNotifSettings).onPreferenceClickListener = this
        findPreference(prefNavMenuSettings).onPreferenceClickListener = this
        findPreference(prefMoreAndCredits).onPreferenceClickListener = this
        findPreference(prefLocationEnabled).onPreferenceClickListener = this
        findPreference(prefSaveData).onPreferenceClickListener = this
        findPreference(prefNotif).onPreferenceClickListener = this
        findPreference(prefLocaleSwitcher).setOnPreferenceChangeListener { _, o ->
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
            prefNotifSettings -> {
                fragmentManager!!.beginTransaction().addToBackStack(null).replace(R.id.content_frame, NotificationsSettingsFragment()).commit()
                return true
            }
            prefNavMenuSettings -> {
                fragmentManager!!.beginTransaction().addToBackStack(null).replace(R.id.content_frame, NavigationMenuFragment()).commit()
                return true
            }
            prefMoreAndCredits -> {
                startActivity(Intent(activity, More::class.java))
                return true
            }
            prefLocationEnabled -> {
                ActivityCompat.requestPermissions(activity!!, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
                return true
            }
            prefSaveData -> {
                setScheduler()
                return true
            }
            prefNotif -> {
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
        if (preferences.getBoolean("notif", false) && !preferences.getBoolean(prefSaveData, false)) {
            scheduler.schedule(preferences.getInt(currentContext.getString(R.string.pref_notif_interval), 60000), true)
        } else {
            scheduler.cancel()
        }
    }
}