package me.zeeroooo.materialfb.misc;

import android.content.Context;

import java.io.File;

public class Utils {

    public static void deleteCache(final Context context) {
        try {
            final File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory())
                deleteDir(dir);
        } catch (Exception e) {
            //
        }
    }

    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            final String[] children = dir.list();
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success)
                    return false;
            }
        }
        return dir.delete();
    }

    private Utils() {
    }
}
