package me.zeeroooo.materialfb.misc

import android.content.Context

import java.io.File

object Utils {

    fun deleteCache(context: Context) {
        try {
            val dir = context.cacheDir
            if (dir != null && dir.isDirectory)
                deleteDir(dir)
        } catch (e: Exception) {
            //
        }

    }

    private fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            for (aChildren in children) {
                val success = deleteDir(File(dir, aChildren))
                if (!success)
                    return false
            }
        }
        return dir!!.delete()
    }
}
