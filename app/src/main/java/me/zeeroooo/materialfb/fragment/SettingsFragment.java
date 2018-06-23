/**
 * Code taken from FaceSlim by indywidualny. Thanks.
 **/
package me.zeeroooo.materialfb.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import me.zeeroooo.materialfb.R;
import me.zeeroooo.materialfb.activity.More;
import me.zeeroooo.materialfb.notification.Scheduler;
import me.zeeroooo.materialfb.ui.CookingAToast;

import java.util.Locale;

public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {

    private SharedPreferences preferences;
    private Scheduler mScheduler;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mScheduler = new Scheduler(getActivity());

        // set onPreferenceClickListener for a few preferences
        findPreference("notifications_settings").setOnPreferenceClickListener(this);
        findPreference("navigation_menu_settings").setOnPreferenceClickListener(this);
        findPreference("moreandcredits").setOnPreferenceClickListener(this);
        findPreference("location_enabled").setOnPreferenceClickListener(this);
        findPreference("save_data").setOnPreferenceClickListener(this);
        findPreference("notif").setOnPreferenceClickListener(this);

        findPreference("localeSwitcher").setOnPreferenceChangeListener((preference, o) -> {
            Locale locale = new Locale(o.toString());
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getResources().updateConfiguration(config, getResources().getDisplayMetrics());
            return true;
        });
    }

    @Override
    public void onCreatePreferences(final Bundle bundle, final String s) {
        // TODO ?
    }

    @Override
    public boolean onPreferenceClick(final Preference preference) {
        switch (preference.getKey()) {
            case "notifications_settings":
                getFragmentManager().beginTransaction().addToBackStack(null).replace(R.id.content_frame, new NotificationsSettingsFragment()).commit();
                return true;
            case "navigation_menu_settings":
                getFragmentManager().beginTransaction().addToBackStack(null).replace(R.id.content_frame, new NavigationMenuFragment()).commit();
                return true;
            case "moreandcredits":
                startActivity(new Intent(getActivity(), More.class));
                return true;
            case "location_enabled":
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                return true;
            case "save_data":
                setScheduler();
                return true;
            case "notif":
                setScheduler();
                return true;
        }
        return false;
    }

    private void setScheduler() {
        if (preferences.getBoolean("notif", false) && !preferences.getBoolean("save_data", false))
            mScheduler.schedule(Integer.parseInt(preferences.getString("notif_interval", "60000")), true);
        else
            mScheduler.cancel();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED))
                    CookingAToast.cooking(getActivity(), getString(R.string.permission_denied), Color.WHITE, Color.parseColor("#ff4444"), R.drawable.ic_error, true).show();
        }
    }
}