/*
 * Code taken from:
 * - FaceSlim by indywidualny. Thanks.
 * - Folio for Facebook by creativetrendsapps. Thanks.
 * - Toffed by JakeLane. Thanks.
 */
package me.zeeroooo.materialfb.activities;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionMenu;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import me.zeeroooo.materialfb.R;
import me.zeeroooo.materialfb.misc.UserInfo;
import me.zeeroooo.materialfb.misc.Utils;
import me.zeeroooo.materialfb.ui.CookingAToast;
import me.zeeroooo.materialfb.ui.Theme;
import me.zeeroooo.materialfb.webview.Helpers;
import me.zeeroooo.materialfb.webview.JavaScriptHelpers;
import me.zeeroooo.materialfb.webview.JavaScriptInterfaces;
import me.zeeroooo.materialfb.webview.MFBWebView;
import me.zeeroooo.materialfb.webview.WebViewClient;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int FILE_CHOOSER_RESULT_CODE = 2888;
    public static final int INPUT_FILE_REQUEST_CODE = 1;

    private SharedPreferences preferences;
    private MFBWebView webView;
    private NavigationView navigationView;
    private DrawerLayout drawer;

    private FloatingActionMenu floatingActionMenu;
    private SwipeRefreshLayout swipeView;

    private String baseURL;
    private String cameraPhotoPath;
    private StringBuilder css = new StringBuilder();

    private ValueCallback<Uri[]> filePathCallback;
    private Uri mCapturedImageURI = null;

    private Uri sharedFromGallery;
    private ValueCallback<Uri> mUploadMessage;
    private String url;
    private String urlIntent = null;

    private DownloadManager mDownloadManager;
    private Handler badgeUpdate;
    private Runnable badgeTask;
    private TextView mostRecentTv, friendsRegTv;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Theme.Temas(this, preferences);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        drawer = findViewById(R.id.drawer_layout);

        // Setup navigation and status bar color
        getWindow().setNavigationBarColor(getApplicationContext().getResources().getColor(R.color.MFBPrimaryDark));
        getWindow().setStatusBarColor(getApplicationContext().getResources().getColor(R.color.MFBPrimary));

        setupBaseUrl();

        switch (preferences.getString("start_url", "Most_recent")) {
            case "Most_recent":
                webView.loadUrl(baseURL + "home.php?sk=h_chr");
                break;
            case "Top_stories":
                webView.loadUrl(baseURL + "home.php?sk=h_nor");
                break;
            case "Messages":
                webView.loadUrl(baseURL + "messages/");
                break;
            default:
                break;
        }

        mostRecentTv = (TextView) navigationView.getMenu().findItem(R.id.nav_most_recent).getActionView();
        friendsRegTv = (TextView) navigationView.getMenu().findItem(R.id.nav_friendreq).getActionView();

        // Hide buttons if they are disabled
        if (!preferences.getBoolean("nav_groups", false))
            navigationView.getMenu().findItem(R.id.nav_groups).setVisible(false);
        if (!preferences.getBoolean("nav_mainmenu", false))
            navigationView.getMenu().findItem(R.id.nav_mainmenu).setVisible(false);
        if (!preferences.getBoolean("nav_most_recent", false))
            navigationView.getMenu().findItem(R.id.nav_most_recent).setVisible(false);
        if (!preferences.getBoolean("nav_events", false))
            navigationView.getMenu().findItem(R.id.nav_events).setVisible(false);
        if (!preferences.getBoolean("nav_photos", false))
            navigationView.getMenu().findItem(R.id.nav_photos).setVisible(false);
        if (!preferences.getBoolean("nav_back", false))
            navigationView.getMenu().findItem(R.id.nav_back).setVisible(false);
        if (!preferences.getBoolean("nav_exitapp", false))
            navigationView.getMenu().findItem(R.id.nav_exitapp).setVisible(false);
        if (!preferences.getBoolean("nav_top_stories", false))
            navigationView.getMenu().findItem(R.id.nav_top_stories).setVisible(false);
        if (!preferences.getBoolean("nav_friendreq", false))
            navigationView.getMenu().findItem(R.id.nav_friendreq).setVisible(false);

        // Start the Swipe to reload listener
        swipeView = findViewById(R.id.swipeLayout);
        swipeView.setColorSchemeResources(android.R.color.white);
        swipeView.setProgressBackgroundColorSchemeColor(Theme.getColor(this));
        swipeView.setOnRefreshListener(() -> webView.reload());

        // Inflate the FAB menu
        floatingActionMenu = findViewById(R.id.menuFAB);

        View.OnClickListener fabOnClickListener = v -> {
            switch (v.getId()) {
                case R.id.textFAB:
                    webView.loadUrl("javascript:(function(){try{document.querySelector('button[name=\"view_overview\"]').click()}catch(_){window.location.href=\"" + baseURL + "?pageload=composer\"}})()");
                    swipeView.setEnabled(false);
                    break;
                case R.id.photoFAB:
                    webView.loadUrl("javascript:(function(){try{document.querySelector('button[name=\"view_photo\"]').click()}catch(_){window.location.href=\"" + baseURL + "?pageload=composer_photo\"}})()");
                    swipeView.setEnabled(false);
                    break;
                case R.id.checkinFAB:
                    webView.loadUrl("javascript:(function(){try{document.querySelector('button[name=\"view_location\"]').click()}catch(_){window.location.href=\"" + baseURL + "?pageload=composer_checkin\"}})()");
                    swipeView.setEnabled(false);
                    break;
                case R.id.topFAB:
                    webView.scrollTo(0, 0);
                    break;
                case R.id.shareFAB:
                    final Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, webView.getUrl());
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.context_share_link)));
                    break;
                default:
                    break;
            }
            floatingActionMenu.close(true);
        };

        findViewById(R.id.textFAB).setOnClickListener(fabOnClickListener);
        findViewById(R.id.photoFAB).setOnClickListener(fabOnClickListener);
        findViewById(R.id.checkinFAB).setOnClickListener(fabOnClickListener);
        findViewById(R.id.topFAB).setOnClickListener(fabOnClickListener);
        findViewById(R.id.shareFAB).setOnClickListener(fabOnClickListener);

        webView.setOnScrollChangedCallback((view, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            // Make sure the hiding is enabled and the scroll was significant
            if (Math.abs(oldScrollY - scrollY) > getApplication().getResources().getDimensionPixelOffset(R.dimen.fab_scroll_threshold)) {
                if (scrollY > oldScrollY) {
                    // User scrolled down, hide the button
                    floatingActionMenu.hideMenuButton(true);
                } else if (scrollY < oldScrollY) {
                    // User scrolled up, show the button
                    floatingActionMenu.showMenuButton(true);
                }
            }
        });

        webView.updateSettings(preferences);
        webView.addJavascriptInterface(new JavaScriptInterfaces(this), "android");
        webView.addJavascriptInterface(this, "Vid");

        webView.setWebViewClient(new WebViewClient(this));
        webView.setWebChromeClient(new me.zeeroooo.materialfb.webview.WebChromeClient(this));

        // Add OnClick listener to Profile picture
        ImageView profileImage = navigationView.getHeaderView(0).findViewById(R.id.profile_picture);
        profileImage.setClickable(true);
        profileImage.setOnClickListener(v -> {
            drawer.closeDrawers();
            webView.loadUrl(baseURL + "me");
        });

        mDownloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        if (getIntent() != null)
            urlIntent(getIntent());
    }

    private void setupBaseUrl() {
        baseURL = (!preferences.getBoolean("save_data", false))
                ? "https://m.facebook.com/"
                : "https://mbasic.facebook.com/";
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    File downloads_dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (!downloads_dir.exists())
                        if (!downloads_dir.mkdirs())
                            return;

                    File destinationFile = new File(downloads_dir, Uri.parse(url).getLastPathSegment());
                    request.setDestinationUri(Uri.fromFile(destinationFile));
                    request.setVisibleInDownloadsUi(true);
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    mDownloadManager.enqueue(request);
                    webView.goBack();
                    CookingAToast.cooking(this, getString(R.string.downloaded), Color.WHITE, Color.parseColor("#00C851"), R.drawable.ic_download, false).show();
                } else
                    CookingAToast.cooking(this, getString(R.string.permission_denied), Color.WHITE, Color.parseColor("#ff4444"), R.drawable.ic_error, true).show();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);

        if (intent.getBooleanExtra("apply", false)) {
            finish();
            Intent apply = new Intent(this, MainActivity.class);
            startActivity(apply);
        }

        urlIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        webView.resumeTimers();

        if (Helpers.getCookie() != null && !preferences.getBoolean("save_data", false)) {
            badgeUpdate = new Handler();
            badgeTask = () -> {
                JavaScriptHelpers.updateNumsService(webView);
                badgeUpdate.postDelayed(badgeTask, 15000);
            };
            badgeTask.run();
            new UserInfo(MainActivity.this).execute();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
        if (badgeTask != null && badgeUpdate != null)
            badgeUpdate.removeCallbacks(badgeTask);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        webView.clearCache(true);
        webView.clearHistory();
        webView.removeAllViews();
        webView.destroy();
        if (badgeTask != null && badgeUpdate != null)
            badgeUpdate.removeCallbacks(badgeTask);
        if (preferences.getBoolean("clear_cache", false))
            Utils.deleteCache(this);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        // Thanks to Koras for the tutorial. http://dev.indywidualni.org/2015/02/an-advanced-webview-with-some-cool-features
        if (Build.VERSION.SDK_INT >= 21) {
            if (requestCode != INPUT_FILE_REQUEST_CODE || filePathCallback == null)
                return;

            Uri[] results = null;

            // Check that the response is a good one
            if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    // If there is not data, then we may have taken a photo
                    if (cameraPhotoPath != null) {
                        results = new Uri[]{Uri.parse(cameraPhotoPath)};
                    }
                } else {
                    String dataString = data.getDataString();
                    if (dataString != null)
                        results = new Uri[]{Uri.parse(dataString)};
                }
            }

            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        } else {
            if (requestCode == FILE_CHOOSER_RESULT_CODE) {
                if (null == this.mUploadMessage)
                    return;

                Uri result;
                if (resultCode != RESULT_OK)
                    result = null;
                else {
                    // retrieve from the private variable if the intent is null
                    result = data == null ? mCapturedImageURI : data.getData();
                }

                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;
            }
        }
    }

    private Point getPointOfView(View view) {
        int[] location = new int[2];
        view.getLocationInWindow(location);
        return new Point(location[0], location[1]);
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START))
            drawer.closeDrawers();
        else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
        setupBaseUrl();
        webView.stopLoading();
        // Handle navigation view item clicks here.
        switch (item.getItemId()) {
            case R.id.nav_top_stories:
                webView.setVisibility(View.INVISIBLE);

                webView.loadUrl(baseURL + "home.php?sk=h_nor");
                setTitle(R.string.menu_top_stories);
                item.setChecked(true);
                break;
            case R.id.nav_most_recent:
                webView.setVisibility(View.INVISIBLE);

                webView.loadUrl(baseURL + "home.php?sk=h_chr'");
                setTitle(R.string.menu_most_recent);
                item.setChecked(true);
                Helpers.uncheckRadioMenu(navigationView.getMenu());
                break;
            case R.id.nav_friendreq:
                webView.setVisibility(View.INVISIBLE);

                webView.loadUrl(baseURL + "friends/center/requests/");
                setTitle(R.string.menu_friendreq);
                item.setChecked(true);
                break;
            case R.id.nav_groups:
                webView.setVisibility(View.INVISIBLE);

                webView.loadUrl(baseURL + "groups/?category=membership");
                css.append("._129- {position:initial}");
                item.setChecked(true);
                break;
            case R.id.nav_mainmenu:
                webView.setVisibility(View.INVISIBLE);

                if (!preferences.getBoolean("save_data", false))
                    webView.loadUrl("javascript:(function()%7Btry%7Bdocument.querySelector('%23bookmarks_jewel%20%3E%20a').click()%7Dcatch(_)%7Bwindow.location.href%3D'" + "https%3A%2F%2Fm.facebook.com%2F" + "home.php'%7D%7D)()");
                else
                    webView.loadUrl("https://mbasic.facebook.com/menu/bookmarks/?ref_component=mbasic_home_header&ref_page=%2Fwap%2Fhome.php&refid=8");
                setTitle(R.string.menu_mainmenu);
                item.setChecked(true);
                break;
            case R.id.nav_events:
                webView.setVisibility(View.INVISIBLE);

                webView.loadUrl(baseURL + "events/");
                css.append("#page{top:0}");
                item.setChecked(true);
                break;
            case R.id.nav_photos:
                webView.setVisibility(View.INVISIBLE);

                webView.loadUrl(baseURL + "photos/");
                break;
            case R.id.nav_settings:
                startActivity(new Intent(this, SettingsActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                break;
            case R.id.nav_back:
                if (webView.canGoBack())
                    webView.goBack();
                break;
            case R.id.nav_exitapp:
                finishAffinity();
                break;
            default:
                break;
        }

        drawer.closeDrawers();
        return true;
    }

    @JavascriptInterface
    @SuppressWarnings("unused")
    public void LoadVideo(final String video_url) {
        Intent Video = new Intent(this, Video.class);
        Video.putExtra("video_url", video_url);
        startActivity(Video);
    }

    public void setRequestsNum(int num) {
        txtFormat(friendsRegTv, num, Color.RED);
    }

    public void setMrNum(int num) {
        txtFormat(mostRecentTv, num, Color.RED);
    }

    private void txtFormat(@NonNull final TextView t, int i, int color) {
        t.setText(String.format("%s", i));
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setTypeface(null, Typeface.BOLD);
        if (i > 0)
            t.setVisibility(View.VISIBLE);
        else
            t.setVisibility(View.INVISIBLE);
    }

    private void urlIntent(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
            if (URLUtil.isValidUrl(intent.getStringExtra(Intent.EXTRA_TEXT))) {
                try {
                    webView.loadUrl("https://mbasic.facebook.com/composer/?text=" + URLEncoder.encode(intent.getStringExtra(Intent.EXTRA_TEXT), "utf-8"));
                } catch (UnsupportedEncodingException uee) {
                    Log.e(TAG, uee.getMessage(), uee);
                }
            }
        }

        if (intent.getExtras() != null)
            urlIntent = intent.getExtras().getString("Job_url");

        if (intent.getDataString() != null) {
            urlIntent = getIntent().getDataString();
            if (intent.getDataString().contains("profile"))
                urlIntent.replace("fb://profile/", "https://facebook.com/");
        }

        if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
            if (intent.getType().startsWith("image/") || intent.getType().startsWith("video/") || intent.getType().startsWith("audio/")) {
                sharedFromGallery = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                css.append("#mbasic_inline_feed_composer{display:initial}");
                webView.loadUrl("https://m.facebook.com");
            }
        }

        String newUrl = urlIntent;
        String more_new_url;
        if (newUrl != null && newUrl.contains("www.facebook.com")) {
            more_new_url = newUrl.replace("www.facebook.com", "m.facebook.com");
            webView.loadUrl(more_new_url);
        } else if (newUrl != null && newUrl.contains("web.facebook.com")) {
            more_new_url = newUrl.replace("web.facebook.com", "m.facebook.com");
            webView.loadUrl(more_new_url);
        } else {
            webView.loadUrl(urlIntent);
        }
    }

    public SwipeRefreshLayout getSwipeView() {
        return swipeView;
    }

    public String getBaseURL() {
        return baseURL;
    }

    public StringBuilder getCss() {
        return css;
    }

    public SharedPreferences getPreferences() {
        return preferences;
    }

    public FloatingActionMenu getFloatingActionMenu() {
        return floatingActionMenu;
    }

    public Uri getSharedFromGallery() {
        return sharedFromGallery;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public void setFilePathCallback(ValueCallback<Uri[]> filePathCallback) {
        this.filePathCallback = filePathCallback;
    }

    public String getCameraPhotoPath() {
        return cameraPhotoPath;
    }

    public void setCameraPhotoPath(String cameraPhotoPath) {
        this.cameraPhotoPath = cameraPhotoPath;
    }
}
