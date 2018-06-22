package me.zeeroooo.materialfb.webview;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

import me.zeeroooo.materialfb.R;
import me.zeeroooo.materialfb.activities.MainActivity;
import me.zeeroooo.materialfb.activities.Photo;

public class WebViewClient extends android.webkit.WebViewClient {

    private final MainActivity activity;
    private Elements elements;

    public WebViewClient(final MainActivity activity) {
        this.activity = activity;
    }

    @SuppressLint("NewApi")
    @Override
    public boolean shouldOverrideUrlLoading(final WebView view, final WebResourceRequest request) {
        return shouldOverrideUrlLoading(view, request.getUrl().toString());
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldOverrideUrlLoading(final WebView view, String url) {
        // clean an url from facebook redirection before processing (no more blank pages on back)
        url = Helpers.cleanAndDecodeUrl(url);

        if (url.contains("mailto:")) {
            Intent mailto = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            activity.startActivity(mailto);
        }

        if ((Uri.parse(url).getHost().endsWith("facebook.com")
                || Uri.parse(url).getHost().endsWith("*.facebook.com")
                || Uri.parse(url).getHost().endsWith("akamaihd.net")
                || Uri.parse(url).getHost().endsWith("ad.doubleclick.net")
                || Uri.parse(url).getHost().endsWith("sync.liverail.com")
                || Uri.parse(url).getHost().endsWith("cdn.fbsbx.com")
                || Uri.parse(url).getHost().endsWith("lookaside.fbsbx.com"))) {
            return false;
        }

        if (url.contains("giphy") || url.contains("gifspace") || url.contains("tumblr") || url.contains("gph.is") || url.contains("gif") || url.contains("fbcdn.net") || url.contains("imgur")) {
            if (url.contains("giphy") || url.contains("gph")) {
                if (!url.endsWith(".gif")) {
                    if (url.contains("giphy.com") || url.contains("html5"))
                        url = String.format("http://media.giphy.com/media/%s/giphy.gif", url.replace("http://giphy.com/gifs/", ""));
                    else if (url.contains("gph.is") && !url.contains("html5")) {
                        view.loadUrl(url);
                        url = String.format("http://media.giphy.com/media/%s/giphy.gif", url.replace("http://giphy.com/gifs/", ""));
                    }

                    if (url.contains("media.giphy.com/media/") && !url.contains("html5")) {
                        String[] giphy = url.split("-");
                        String giphy_id = giphy[giphy.length - 1];
                        url = "http://media.giphy.com/media/" + giphy_id;
                    }
                    if (url.contains("media.giphy.com/media/http://media")) {
                        String[] gph = url.split("/");
                        String gph_id = gph[gph.length - 2];
                        url = "http://media.giphy.com/media/" + gph_id + "/giphy.gif";
                    }
                    if (url.contains("html5/giphy.gif")) {
                        String[] giphy_html5 = url.split("/");
                        String giphy_html5_id = giphy_html5[giphy_html5.length - 3];
                        url = "http://media.giphy.com/media/" + giphy_html5_id + "/giphy.gif";
                    }
                }
                if (url.contains("?")) {
                    String[] giphy1 = url.split("\\?");
                    String giphy_html5_id = giphy1[0];
                    url = giphy_html5_id + "/giphy.gif";
                }
            }

            if (url.contains("gifspace")) {
                if (!url.endsWith(".gif"))
                    url = String.format("http://gifspace.net/image/%s.gif", url.replace("http://gifspace.net/image/", ""));
            }

            if (url.contains("phygee")) {
                if (!url.endsWith(".gif")) {
                    getSrc(url, "span", "img");
                    url = "http://www.phygee.com/" + elements.attr("src");
                }
            }

            if (url.contains("imgur")) {
                if (!url.endsWith(".gif") && !url.endsWith(".jpg")) {
                    getSrc(url, "div.post-image", "img");
                    url = "https:" + elements.attr("src");
                }
            }

            if (url.contains("media.upgifs.com")) {
                if (!url.endsWith(".gif")) {
                    getSrc(url, "div.gif-pager-container", "img#main-gif");
                    url = elements.attr("src");
                }
            }

            imageLoader(url, view);
            return true;
        } else {
            // Open external links in browser
            Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            activity.startActivity(browser);
            return true;
        }
    }

    @Override
    public void onPageStarted(final WebView view, final String url, final Bitmap favicon) {
        activity.getSwipeView().setRefreshing(true);
        if (url.contains("https://mbasic.facebook.com/home.php?s="))
            view.loadUrl(activity.getBaseURL());
    }

    @Override
    public void onLoadResource(final WebView view, final String url) {
        JavaScriptHelpers.videoView(view);
        if (activity.getSwipeView().isRefreshing())
            JavaScriptHelpers.loadCSS(view, activity.getCss().toString());
        if (url.contains("facebook.com/composer/mbasic/") || url.contains("https://m.facebook.com/sharer.php?sid="))
            activity.getCss().append("#page{top:0}");

        if (url.contains("/photos/viewer/"))
            imageLoader(activity.getBaseURL() + "photo/view_full_size/?fbid=" + url.substring(url.indexOf("photo=") + 6).split("&")[0], view);

        if (url.contains("/photo/view_full_size/?fbid="))
            imageLoader(url.split("&ref_component")[0], view);
    }

    @Override
    public void onPageFinished(final WebView view, final String url) {
        activity.getSwipeView().setRefreshing(false);

        switch (activity.getPreferences().getString("web_themes", "Material")) {
            case "FacebookMobile":
                break;
            case "Material":
                activity.getCss().append(activity.getString(R.string.Material));
                break;
            case "MaterialAmoled":
                activity.getCss().append(activity.getString(R.string.MaterialAmoled));
                activity.getCss().append("::selection {background: #D3D3D3;}");
                break;
            case "MaterialBlack":
                activity.getCss().append(activity.getString(R.string.MaterialBlack));
                break;
            case "MaterialPink":
                activity.getCss().append(activity.getString(R.string.MaterialPink));
                break;
            case "MaterialGrey":
                activity.getCss().append(activity.getString(R.string.MaterialGrey));
                break;
            case "MaterialGreen":
                activity.getCss().append(activity.getString(R.string.MaterialGreen));
                break;
            case "MaterialRed":
                activity.getCss().append(activity.getString(R.string.MaterialRed));
                break;
            case "MaterialLime":
                activity.getCss().append(activity.getString(R.string.MaterialLime));
                break;
            case "MaterialYellow":
                activity.getCss().append(activity.getString(R.string.MaterialYellow));
                break;
            case "MaterialPurple":
                activity.getCss().append(activity.getString(R.string.MaterialPurple));
                break;
            case "MaterialLightBlue":
                activity.getCss().append(activity.getString(R.string.MaterialLightBlue));
                break;
            case "MaterialOrange":
                activity.getCss().append(activity.getString(R.string.MaterialOrange));
                break;
            case "MaterialGooglePlayGreen":
                activity.getCss().append(activity.getString(R.string.MaterialGPG));
                break;
            default:
                break;
        }

        if (url.contains("lookaside") || url.contains("cdn.fbsbx.com")) {
            activity.setUrl(url);
            RequestStoragePermission();
        }

        // Enable or disable FAB
        if (url.contains("messages") || !activity.getPreferences().getBoolean("fab_enable", false))
            activity.getFloatingActionMenu().setVisibility(View.GONE);
        else
            activity.getFloatingActionMenu().setVisibility(View.VISIBLE);

        if (url.contains("https://mbasic.facebook.com/composer/?text=")) {
            final UrlQuerySanitizer sanitizer = new UrlQuerySanitizer();
            sanitizer.setAllowUnregisteredParamaters(true);
            sanitizer.parseUrl(url);
            final String param = sanitizer.getValue("text");
            view.loadUrl("javascript:(function(){document.querySelector('#composerInput').innerHTML='" + param + "'})()");
        }

        if (url.contains("https://m.facebook.com/public/")) {
            String[] user = url.split("/");
            String profile = user[user.length - 1];
            view.loadUrl("javascript:(function(){document.querySelector('input#u_0_0._5whq.input').value='" + profile + "'})()");
            view.loadUrl("javascript:(function(){try{document.querySelector('button#u_0_1.btn.btnD.mfss.touchable').disabled = false}catch(_){}})()");
            view.loadUrl("javascript:(function(){try{document.querySelector('button#u_0_1.btn.btnD.mfss.touchable').click()}catch(_){}})()");
        }

        if (activity.getPreferences().getBoolean("hide_menu_bar", true))
            activity.getCss().append("#page{top:-45px}");
        // Hide the status editor on the News Feed if setting is enabled
        if (activity.getPreferences().getBoolean("hide_editor_newsfeed", true))
            activity.getCss().append("#mbasic_inline_feed_composer{display:none}");

        // Hide the top story panel in news feed right before the status box
        if (activity.getPreferences().getBoolean("hide_top_story", true))
            activity.getCss().append("._59e9._55wr._4g33._400s{display:none}");

        // Hide 'Sponsored' content (ads)
        if (activity.getPreferences().getBoolean("hide_sponsored", true))
            activity.getCss().append("article[data-ft*=ei]{display:none}");

        // Hide birthday content from News Feed
        if (activity.getPreferences().getBoolean("hide_birthdays", true))
            activity.getCss().append("article#u_1j_4{display:none}article._55wm._5e4e._5fjt{display:none}");

        if (activity.getPreferences().getBoolean("comments_recently", true))
            activity.getCss().append("._15ks+._4u3j{display:none}");

        if (activity.getSharedFromGallery() != null)
            view.loadUrl("javascript:(function(){try{document.getElementsByClassName(\"_56bz _54k8 _52jh _5j35 _157e\")[0].click()}catch(_){document.getElementsByClassName(\"_50ux\")[0].click()}})()");

        activity.getCss().append("article#u_0_q._d2r{display:none}*{-webkit-tap-highlight-color:transparent;outline:0}");
    }

    private void getSrc(final String url, final String select, final String select2) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void[] params) {
                try {
                    Document document = Jsoup.connect(url).get();
                    elements = document.select(select).select(select2);
                } catch (IOException ioex) {
                    ioex.getStackTrace();
                }
                return null;
            }
        }.execute();
    }

    private void imageLoader(final String url, final WebView view) {
        activity.startActivity(new Intent(activity, Photo.class).putExtra("link", url).putExtra("title", view.getTitle()));
        view.stopLoading();
        view.goBack();
    }

    private void RequestStoragePermission() {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
    }
}
