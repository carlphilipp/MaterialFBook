package me.zeeroooo.materialfb.misc

object Constant {
    const val INPUT_FILE_REQUEST_CODE = 1

    object Url {
        private const val PROTOCOL = "https"
        private const val HOST_DESKTOP = "www"
        private const val HOST_MOBILE = "m"
        private const val HOST_MBASIC = "mbasic"
        private const val HOST_WEB = "web"

        const val DOMAIN = "facebook.com"
        const val DESKTOP_URL = "$HOST_DESKTOP.$DOMAIN"
        const val MOBILE_URL = "$HOST_MOBILE.$DOMAIN"
        private const val MBASIC_URL = "$HOST_MBASIC.$DOMAIN"
        const val WEB_URL = "$HOST_WEB.$DOMAIN"

        private const val DESKTOP_FULL_URL = "$PROTOCOL://$DESKTOP_URL"
        const val DESKTOP_ME_FULL_URL = "$DESKTOP_FULL_URL/me"
        const val MOBILE_FULL_URL = "$PROTOCOL://$MOBILE_URL"
        const val MBASIC_FULL_URL = "$PROTOCOL://$MBASIC_URL"
    }

    object Preference {
        const val APPLY = "apply"
        const val JOB_URL = "Job_url"
        const val NOTIF_INTERVAL = "notif_interval" //
        const val VIDEO_URL = "video_url"
        const val SAVE_DATA = "save_data"
    }
}