/*
 * Code taken from FaceSlim by indywidualny. Thanks.
 */
package me.zeeroooo.materialfb.fragment

import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ListView
import me.zeeroooo.materialfb.R
import me.zeeroooo.materialfb.misc.BlackListH
import me.zeeroooo.materialfb.misc.BlacklistAdapter
import me.zeeroooo.materialfb.misc.DatabaseHelper
import me.zeeroooo.materialfb.notification.Scheduler
import java.util.ArrayList

class NotificationsSettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {

    private lateinit var currentContext: Context
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var cursor: Cursor
    private lateinit var preferences: SharedPreferences
    private lateinit var adapter: BlacklistAdapter
    private val blackList: MutableList<BlackListH> = ArrayList()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.currentContext = context
    }

    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.notifications_settings)
        preferences = PreferenceManager.getDefaultSharedPreferences(currentContext)
        databaseHelper = DatabaseHelper(currentContext)
        cursor = databaseHelper.readableDatabase.rawQuery("SELECT BL FROM mfb_table", null)
        adapter = BlacklistAdapter(activity, blackList, databaseHelper)

        findPreference("BlackList").onPreferenceClickListener = this

        while (cursor.moveToNext()) {
            if (cursor.getString(0) != null) {
                blackList.add(BlackListH(cursor.getString(0)))
            }
        }

        preferences.registerOnSharedPreferenceChangeListener { prefs, key ->
            when (key) {
                "notif_interval" -> reschedule(prefs)
                "notif_exact" -> reschedule(prefs)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!cursor.isClosed) {
            databaseHelper.close()
            cursor.close()
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            "BlackList" -> {
                val blacklistDialog = AlertDialog.Builder(activity!!)
                val inflater = LayoutInflater.from(activity)
                val view = inflater.inflate(R.layout.blacklist, null)
                val blword = view.findViewById<EditText>(R.id.blword_new)
                blacklistDialog.setView(view)
                blacklistDialog.setTitle(R.string.blacklist_title)

                val blackListView = view.findViewById<ListView>(R.id.BlackListView)
                blackListView.adapter = adapter

                blacklistDialog.setPositiveButton(android.R.string.ok) { dialog, id ->
                    val word = blword.text.toString()
                    if (word != "") {
                        val blackListH = BlackListH(word)
                        databaseHelper.addData(null, null, blackListH!!.word)
                        blackList.add(blackListH)
                        adapter.notifyDataSetChanged()
                    }
                }
                blacklistDialog.setCancelable(false)
                blacklistDialog.show()
                return true
            }
        }
        return false
    }

    override fun onResume() {
        super.onResume()

        // update notification ringtone preference summary
        var ringtoneString = preferences!!.getString("ringtone", "content://settings/system/notification_sound")
        var ringtoneUri = Uri.parse(ringtoneString)
        var name: String

        name = try {
            val ringtone = RingtoneManager.getRingtone(activity, ringtoneUri)
            ringtone.getTitle(activity)
        } catch (ex: Exception) {
            ex.printStackTrace()
            "Default"
        }

        if ("" == ringtoneString)
            name = getString(R.string.silent)

        val rpn = findPreference("ringtone")
        rpn.summary = getString(R.string.notification_sound_description) + name

        // update message ringtone preference summary
        ringtoneString = preferences!!.getString("ringtone_msg", "content://settings/system/notification_sound")
        ringtoneUri = Uri.parse(ringtoneString)

        name = try {
            val ringtone = RingtoneManager.getRingtone(activity, ringtoneUri)
            ringtone.getTitle(activity)
        } catch (ex: Exception) {
            ex.printStackTrace()
            "Default"
        }

        if ("" == ringtoneString)
            name = getString(R.string.silent)

        val rpm = findPreference("ringtone_msg")
        rpm.summary = getString(R.string.notification_sound_description) + name
    }

    private fun reschedule(preferences: SharedPreferences) {
        val mScheduler = Scheduler(activity)
        mScheduler.cancel()
        mScheduler.schedule(Integer.parseInt(preferences.getString("notif_interval", "60000")), true)
    }
}