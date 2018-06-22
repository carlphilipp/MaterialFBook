package me.zeeroooo.materialfb.Misc;

import android.os.AsyncTask;
import android.util.Log;
import android.webkit.CookieManager;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import me.zeeroooo.materialfb.Activities.MainActivity;
import me.zeeroooo.materialfb.R;
import me.zeeroooo.materialfb.WebView.Helpers;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

public class UserInfo extends AsyncTask<Void, Void, String> {

    private static final String TAG = UserInfo.class.getSimpleName();

    private final MainActivity activity;
    private String name, cover;

    public UserInfo(final MainActivity activity) {
        this.activity = activity;
    }

    @Override
    protected String doInBackground(Void[] params) {
        try {
            Element e = Jsoup.connect("https://www.facebook.com/me").cookie(("https://m.facebook.com"), CookieManager.getInstance().getCookie(("https://m.facebook.com"))).timeout(300000).get().body();
            name = e.select("input[name=q]").attr("value");
            cover = Helpers.decodeImg(e.toString().split("<img class=\"coverPhotoImg photo img\" src=\"")[1].split("\"")[0]);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(String string) {
        try {
            if (name != null)
                ((TextView) activity.findViewById(R.id.profile_name)).setText(name);
            if (cover != null)
                Glide.with(activity)
                        .load(cover)
                        .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                        .into((ImageView) activity.findViewById(R.id.cover));
            if (Helpers.getCookie() != null && activity.findViewById(R.id.profile_picture) != null)
                Glide.with(activity)
                        .load("https://graph.facebook.com/" + Helpers.getCookie() + "/picture?type=large")
                        .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL).circleCrop())
                        .into((ImageView) activity.findViewById(R.id.profile_picture));
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }
}