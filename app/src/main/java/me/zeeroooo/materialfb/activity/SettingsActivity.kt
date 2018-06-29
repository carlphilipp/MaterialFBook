/*
 * Code taken from:
 * - FaceSlim by indywidualny. Thanks.
 * - Toffed by JakeLane. Thanks.
 */
package me.zeeroooo.materialfb.activity

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import me.zeeroooo.materialfb.fragment.SettingsFragment
import me.zeeroooo.materialfb.ui.Theme
import me.zeeroooo.materialfb.R
import me.zeeroooo.materialfb.misc.Constant.Preference.APPLY

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        Theme.temas(this, preferences)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        window.navigationBarColor = applicationContext.getColor(R.color.MFBPrimaryDark)
        window.statusBarColor = applicationContext.getColor(R.color.MFBPrimary)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportFragmentManager.beginTransaction().replace(R.id.content_frame, SettingsFragment()).commit()
        }
    }

    override fun onBackPressed() {
        val count = supportFragmentManager.backStackEntryCount
        if (count == 0) {
            val apply = Intent(this, MainActivity::class.java)
            apply.putExtra(APPLY, true)
            startActivity(apply)
            finish()
        } else
            supportFragmentManager.popBackStack()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return false
    }
}