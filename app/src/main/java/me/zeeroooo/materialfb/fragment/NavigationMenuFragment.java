package me.zeeroooo.materialfb.fragment;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

import me.zeeroooo.materialfb.R;

public class NavigationMenuFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.navigation_menu_settings);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        // TODO ?
    }
}