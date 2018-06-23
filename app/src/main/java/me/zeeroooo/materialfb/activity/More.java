package me.zeeroooo.materialfb.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import me.zeeroooo.materialfb.R;
import me.zeeroooo.materialfb.ui.Theme;

public class More extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Theme.Temas(this, mPreferences);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more);
        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
    }

    public static class MoreAndCredits extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {
        @Override
        public void onCreate(final Bundle Instance) {
            super.onCreate(Instance);
            addPreferencesFromResource(R.xml.more);
            findPreference("changelog").setOnPreferenceClickListener(this);
            findPreference("mfb_version").setSummary(getString(R.string.updates_summary));
        }

        @Override
        public void onCreatePreferences(final Bundle bundle, final String s) {

        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            switch (preference.getKey()) {
                case "changelog":
                    AlertDialog.Builder changelog = new AlertDialog.Builder(getContext());
                    changelog.setTitle(getResources().getString(R.string.changelog));
                    changelog.setMessage(Html.fromHtml(getResources().getString(R.string.changelog_list)));
                    changelog.setCancelable(false);
                    changelog.setPositiveButton("Ok!", (changelog1, id) -> {
                        // Nothing here :p
                    });
                    changelog.show();
                    return true;
            }
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
