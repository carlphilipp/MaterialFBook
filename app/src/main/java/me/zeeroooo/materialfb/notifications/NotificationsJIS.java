package me.zeeroooo.materialfb.notifications;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.webkit.CookieManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import me.zeeroooo.materialfb.activities.MainActivity;
import me.zeeroooo.materialfb.misc.DatabaseHelper;
import me.zeeroooo.materialfb.R;
import me.zeeroooo.materialfb.ui.Theme;
import me.zeeroooo.materialfb.webview.Helpers;

public class NotificationsJIS extends JobIntentService {
    private SharedPreferences mPreferences;
    private boolean msg_notAWhiteList = false, notif_notAWhiteList = false;
    private String baseURL, pictureNotif, pictureMsg, e = "";
    private Bitmap picprofile;
    private String[] picMsg, picNotif;
    private Spanned emoji;
    private int mode = 0;
    private List<String> blist;
    private DatabaseHelper db;
    private Cursor cursor;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, NotificationsJIS.class, 2, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        db = new DatabaseHelper(this);
        Log.i("JobIntentService_MFB", "Started");
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        URLs();
        blist = new ArrayList<>();

        try {
            cursor = db.getReadableDatabase().rawQuery("SELECT BL FROM mfb_table", null);
            while (cursor != null && cursor.moveToNext())
                if (cursor.getString(0) != null)
                    blist.add(cursor.getString(0));
            if (mPreferences.getBoolean("facebook_messages", false))
                SyncMessages();
            if (mPreferences.getBoolean("facebook_notifications", false))
                SyncNotifications();
        } catch (Exception e) {
            e.getStackTrace();
        }
    }

    private void URLs() {
        if (!mPreferences.getBoolean("save_data", false))
            baseURL = "https://m.facebook.com/";
        else
            baseURL = "https://mbasic.facebook.com/";
    }

    @Override
    public void onDestroy() {
        Log.i("JobIntentService_MFB", "Stopped");
        if (pictureMsg != null)
            pictureMsg = "";
        if (pictureNotif != null)
            pictureNotif = "";
        if (!cursor.isClosed()) {
            db.close();
            cursor.close();
        }
        if (msg_notAWhiteList)
            msg_notAWhiteList = false;
        if (notif_notAWhiteList)
            notif_notAWhiteList = false;
        super.onDestroy();
    }

    // Sync the notifications
    void SyncNotifications() throws Exception {
        Log.i("JobIntentService_MFB", "Trying: " + "https://m.facebook.com/notifications.php");
        Document doc = Jsoup.connect("https://m.facebook.com/notifications.php").cookie(("https://m.facebook.com"), CookieManager.getInstance().getCookie(("https://m.facebook.com"))).timeout(300000).get();
        Element notifications = doc.selectFirst("div.aclb > div.touchable-notification > a.touchable");

        final String time = notifications.select("span.mfss.fcg").text();
        final String content = notifications.select("div.c").text().replace(time, "");
        if (!blist.isEmpty())
            for (int listCount = 0; listCount < blist.size(); listCount++) {
                if (content.contains(blist.get(listCount)))
                    notif_notAWhiteList = true;
            }
        if (!notif_notAWhiteList) {
            final String text = content.replace(time, "");
            pictureNotif = notifications.select("i.img.l.profpic").attr("style");

            if (pictureNotif != null)
                picNotif = pictureNotif.split("('*')");

            if (!mPreferences.getString("last_notification_text", "").contains(text))
                notifier(content, getString(R.string.app_name), baseURL + "/notifications.php", picNotif[1], 12);

            mPreferences.edit().putString("last_notification_text", text).apply();
        }
    }

    void SyncMessages() throws Exception {
        Log.i("JobIntentService_MFB", "Trying: " + "https://m.facebook.com/messages?soft=messages");
        Document doc = Jsoup.connect("https://m.facebook.com/messages?soft=messages").cookie(("https://m.facebook.com"), CookieManager.getInstance().getCookie(("https://m.facebook.com"))).timeout(300000).get();
        Element result = doc.getElementsByClass("item messages-flyout-item aclb abt").select("a.touchable.primary").first();
        if (result != null) {
            final String content = result.select("div.oneLine.preview.mfss.fcg").text();
            if (!blist.isEmpty())
                for (String s : blist) {
                    if (content.contains(s))
                        msg_notAWhiteList = true;
                }
            if (!msg_notAWhiteList) {
                final String text = result.text().replace(result.select("div.time.r.nowrap.mfss.fcl").text(), "");
                final String name = result.select("div.title.thread-title.mfsl.fcb").text();
                pictureMsg = result.select("i.img.profpic").attr("style");
                String CtoDisplay = content;

                if (pictureMsg != null)
                    picMsg = pictureMsg.split("('*')");

                Elements e_iemoji = result.select("._47e3._3kkw");
                if (!e_iemoji.isEmpty())
                    for (Element em : e_iemoji) {
                        String emojiUrl = em.attr("style");
                        String[] emoji_sp = emojiUrl.split("/");
                        String emoji_unicode = "0x" + emoji_sp[9].replace(".png)", "");
                        int i = Integer.parseInt(emoji_unicode.substring(2), 16);
                        String emoji_char = new String(Character.toChars(i));
                        e = e + emoji_char;
                        emoji = Html.fromHtml(e);
                        mode = 1;
                    }
                Elements e_emoji = result.select("._1ift._2560.img");
                if (!e_emoji.isEmpty())
                    for (Element em : e_emoji) {
                        String emojiUrl = em.attr("src");
                        String[] emoji_sp = emojiUrl.split("/");
                        String emoji_unicode = "0x" + emoji_sp[9].replace(".png", "");
                        int i = Integer.parseInt(emoji_unicode.substring(2), 16);
                        String emoji_char = new String(Character.toChars(i));
                        e = e + emoji_char;
                        emoji = Html.fromHtml(e);
                        mode = 2;
                    }

                if (mode != 0)
                    CtoDisplay += " " + emoji;

                if (!mPreferences.getString("last_message", "").equals(text))
                    notifier(CtoDisplay, name, baseURL + "messages/", picMsg[1], 969);

                // save as shown (or ignored) to avoid showing it again
                mPreferences.edit().putString("last_message", text).apply();
            }
        }
    }

    // create a notification and display it
    private void notifier(@Nullable final String content, final String title, final String url, final String image_url, int id) {

        try {
            picprofile = Glide.with(this).asBitmap().load(Helpers.decodeImg(image_url)).apply(RequestOptions.circleCropTransform()).into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).get();
        } catch (Exception e) {
            e.getStackTrace();
        }

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        String channelId;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // create all channels at once so users can see/configure them all with the first notification
            NotificationChannel messagesChannel = createNotificationChannel(mNotificationManager, "me.zeeroooo.materialfb.notif.messages", getString(R.string.facebook_message), "vibrate_msg", "vibrate_double_msg", "led_msj");
            NotificationChannel facebookChannel = createNotificationChannel(mNotificationManager, "me.zeeroooo.materialfb.notif.facebook", getString(R.string.facebook_notifications), "vibrate_notif", "vibrate_double_notif", "led_notif");

            if (id == 969)
                channelId = messagesChannel.getId();
            else
                channelId = facebookChannel.getId();
        } else {
            channelId = "me.zeeroooo.materialfb.notif";
        }

        String ringtoneKey, vibrate_, vibrate_double_, led_;

        if (id == 969) {
            ringtoneKey = "ringtone_msg";
            vibrate_ = "vibrate_msg";
            vibrate_double_ = "vibrate_double_msg";
            led_ = "led_msj";
        } else {
            ringtoneKey = "ringtone";
            vibrate_ = "vibrate_notif";
            vibrate_double_ = "vibrate_double_notif";
            led_ = "led_notif";
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, channelId)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setColor(Theme.getColor(this))
                .setContentTitle(title)
                .setContentText(content)
                .setWhen(System.currentTimeMillis())
                .setLargeIcon(picprofile)
                .setSmallIcon(R.mipmap.ic_launcher_responsive)
                .setAutoCancel(true);

        if (mPreferences.getBoolean(vibrate_, false)) {
            mBuilder.setVibrate(new long[]{500, 500});
            if (mPreferences.getBoolean(vibrate_double_, false))
                mBuilder.setVibrate(new long[]{500, 500, 500, 500});
        }

        if (mPreferences.getBoolean(led_, false))
            mBuilder.setLights(Color.BLUE, 1000, 1000);

        // priority for Heads-up
        mBuilder.setPriority(Notification.PRIORITY_HIGH);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            mBuilder.setCategory(Notification.CATEGORY_MESSAGE);

        Uri ringtoneUri = Uri.parse(mPreferences.getString(ringtoneKey, "content://settings/system/notification_sound"));
        mBuilder.setSound(ringtoneUri);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("Job_url", url);
        mBuilder.setOngoing(false);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        mBuilder.setContentIntent(resultPendingIntent);

        if (mNotificationManager != null)
            if (id == 969)
                mNotificationManager.notify(969, mBuilder.build());
            else
                mNotificationManager.notify(id, mBuilder.build());
    }

    @TargetApi(26)
    private NotificationChannel createNotificationChannel(NotificationManager notificationManager, String id, String name, String vibratePref, String doubleVibratePref, String ledPref) {
        NotificationChannel notificationChannel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH);
        notificationChannel.setShowBadge(true);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationChannel.enableVibration(mPreferences.getBoolean(vibratePref, false));
        notificationChannel.enableLights(mPreferences.getBoolean(ledPref, false));
        if (mPreferences.getBoolean(vibratePref, false)) {
            notificationChannel.setVibrationPattern(new long[]{500, 500});
            if (mPreferences.getBoolean(doubleVibratePref, false))
                notificationChannel.setVibrationPattern(new long[]{500, 500, 500, 500});
        }
        if (mPreferences.getBoolean(ledPref, false))
            notificationChannel.setLightColor(Color.BLUE);
        notificationManager.createNotificationChannel(notificationChannel);

        return notificationChannel;
    }

    public static void ClearbyId(Context c, int id) {
        NotificationManager mNotificationManager = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (mNotificationManager != null)
            if (id == 969)
                mNotificationManager.cancel(id);
            else
                mNotificationManager.cancelAll();
    }
}
