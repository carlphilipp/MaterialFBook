package me.zeeroooo.materialfb.activity

import android.app.Application
import me.zeeroooo.materialfb.service.PreferenceServices

/**
 * Created by ZeeRooo on 15/04/18
 */
class App : Application() {

    val preferenceService by lazy { PreferenceServices(applicationContext) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: App
    }
}
