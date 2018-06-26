package me.zeeroooo.materialfb.activity

import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.widget.Toolbar
import android.text.Html
import me.zeeroooo.materialfb.R
import me.zeeroooo.materialfb.ui.Theme

class More : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val mPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        Theme.Temas(this, mPreferences)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_more)
        val mToolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(mToolbar)
    }

    override fun onBackPressed() {
        finish()
    }

    class MoreAndCredits : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {
        override fun onCreate(bundle: Bundle?) {
            super.onCreate(bundle)
            addPreferencesFromResource(R.xml.more)
            findPreference("changelog").onPreferenceClickListener = this
            findPreference("mfb_version").summary = getString(R.string.updates_summary)
        }

        override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        }

        override fun onPreferenceClick(preference: Preference): Boolean {
            when (preference.key) {
                "changelog" -> {
                    val changelog = AlertDialog.Builder(context!!)
                    changelog.setTitle(resources.getString(R.string.changelog))
                    changelog.setMessage(Html.fromHtml(resources.getString(R.string.changelog_list)))
                    changelog.setCancelable(false)
                    changelog.setPositiveButton("Ok!") { _, _ ->
                        // Nothing here :p
                    }
                    changelog.show()
                    return true
                }
            }
            return false
        }
    }
}