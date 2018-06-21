package me.zeeroooo.materialfb.Activities;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;

import me.zeeroooo.materialfb.BuildConfig;
import me.zeeroooo.materialfb.Ui.Theme;
import me.zeeroooo.materialfb.R;

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

    public static class MoreAndCredits extends PreferenceFragment implements Preference.OnPreferenceClickListener {
        @Override
        public void onCreate(final Bundle Instance) {
            super.onCreate(Instance);
            addPreferencesFromResource(R.xml.more);
            findPreference("changelog").setOnPreferenceClickListener(this);
            findPreference("mfb_version").setSummary(getString(R.string.updates_summary, BuildConfig.VERSION_NAME));
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            switch (preference.getKey()) {
                case "changelog":
                    AlertDialog.Builder changelog = new AlertDialog.Builder(getActivity());
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
