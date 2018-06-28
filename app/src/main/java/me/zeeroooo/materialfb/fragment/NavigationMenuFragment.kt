package me.zeeroooo.materialfb.fragment

import android.os.Bundle
import android.support.v7.preference.PreferenceFragmentCompat

import me.zeeroooo.materialfb.R

class NavigationMenuFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.navigation_menu_settings)
    }
}