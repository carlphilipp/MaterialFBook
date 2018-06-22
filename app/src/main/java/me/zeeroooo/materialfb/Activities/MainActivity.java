/*
 * Code taken from:
 * - FaceSlim by indywidualny. Thanks.
 * - Folio for Facebook by creativetrendsapps. Thanks.
 * - Toffed by JakeLane. Thanks.
 */
package me.zeeroooo.materialfb.Activities;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionMenu;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import me.zeeroooo.materialfb.Misc.UserInfo;
import me.zeeroooo.materialfb.Notifications.NotificationsJIS;
import me.zeeroooo.materialfb.R;
import me.zeeroooo.materialfb.Ui.CookingAToast;
import me.zeeroooo.materialfb.Ui.Theme;
import me.zeeroooo.materialfb.WebView.Helpers;
import me.zeeroooo.materialfb.WebView.JavaScriptHelpers;
import me.zeeroooo.materialfb.WebView.JavaScriptInterfaces;
import me.zeeroooo.materialfb.WebView.MFBWebView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final int FILE_CHOOSER_RESULT_CODE = 2888;
    private static final int INPUT_FILE_REQUEST_CODE = 1;

    private SharedPreferences preferences;
    private MFBWebView webView;
    private NavigationView navigationView;
    private DrawerLayout drawer;

    private ValueCallback<Uri[]> mFilePathCallback;
    private Uri mCapturedImageURI = null, sharedFromGallery;
    private ValueCallback<Uri> mUploadMessage;
    private String baseURL, mCameraPhotoPath, Url, css, urlIntent = null;
    private SwipeRefreshLayout swipeView;
    private FloatingActionMenu mMenuFAB;
    private DownloadManager mDownloadManager;
    private Elements elements;
    private Handler badgeUpdate;
    private Runnable badgeTask;
    private TextView mostRecentTv, friendsRegTv;
    private boolean showAnimation;
    private View circleRevealView;

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(getApplicationContext().getResources().getColor(R.color.MFBPrimaryDark));
            getWindow().setStatusBarColor(getApplicationContext().getResources().getColor(R.color.MFBPrimary));
        }

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
        mMenuFAB = findViewById(R.id.menuFAB);

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
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, webView.getUrl());
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.context_share_link)));
                    break;
                default:
                    break;
            }
            mMenuFAB.close(true);
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
                    mMenuFAB.hideMenuButton(true);
                } else if (scrollY < oldScrollY) {
                    // User scrolled up, show the button
                    mMenuFAB.showMenuButton(true);
                }
            }
        });

        webView.getSettings().setGeolocationEnabled(preferences.getBoolean("location_enabled", false));
        webView.getSettings().setMinimumFontSize(Integer.parseInt(preferences.getString("textScale", "1")));
        webView.addJavascriptInterface(new JavaScriptInterfaces(this), "android");
        webView.addJavascriptInterface(this, "Vid");
        webView.getSettings().setBlockNetworkImage(preferences.getBoolean("stop_images", false));
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

        if (Build.VERSION.SDK_INT >= 19)
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        else
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        webView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        webView.getSettings().setUserAgentString("Mozilla/5.0 (X11; Linux ARM) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36");
        webView.setWebViewClient(new WebViewClient() {
            @SuppressLint("NewApi")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return shouldOverrideUrlLoading(view, request.getUrl().toString());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // clean an url from facebook redirection before processing (no more blank pages on back)
                url = Helpers.cleanAndDecodeUrl(url);

                if (url.contains("mailto:")) {
                    Intent mailto = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(mailto);
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
                    startActivity(browser);
                    return true;
                }
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

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                swipeView.setRefreshing(true);
                if (url.contains("https://mbasic.facebook.com/home.php?s="))
                    view.loadUrl(baseURL);
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                JavaScriptHelpers.videoView(view);
                if (swipeView.isRefreshing())
                    JavaScriptHelpers.loadCSS(view, css);
                if (url.contains("facebook.com/composer/mbasic/") || url.contains("https://m.facebook.com/sharer.php?sid="))
                    css += "#page{top:0}";

                if (url.contains("/photos/viewer/"))
                    imageLoader(baseURL + "photo/view_full_size/?fbid=" + url.substring(url.indexOf("photo=") + 6).split("&")[0], view);

                if (url.contains("/photo/view_full_size/?fbid="))
                    imageLoader(url.split("&ref_component")[0], view);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                swipeView.setRefreshing(false);

                switch (preferences.getString("web_themes", "Material")) {
                    case "FacebookMobile":
                        break;
                    case "Material":
                        css += getString(R.string.Material);
                        break;
                    case "MaterialAmoled":
                        css += getString(R.string.MaterialAmoled);
                        css += "::selection {background: #D3D3D3;}";
                        break;
                    case "MaterialBlack":
                        css += getString(R.string.MaterialBlack);
                        break;
                    case "MaterialPink":
                        css += getString(R.string.MaterialPink);
                        break;
                    case "MaterialGrey":
                        css += getString(R.string.MaterialGrey);
                        break;
                    case "MaterialGreen":
                        css += getString(R.string.MaterialGreen);
                        break;
                    case "MaterialRed":
                        css += getString(R.string.MaterialRed);
                        break;
                    case "MaterialLime":
                        css += getString(R.string.MaterialLime);
                        break;
                    case "MaterialYellow":
                        css += getString(R.string.MaterialYellow);
                        break;
                    case "MaterialPurple":
                        css += getString(R.string.MaterialPurple);
                        break;
                    case "MaterialLightBlue":
                        css += getString(R.string.MaterialLightBlue);
                        break;
                    case "MaterialOrange":
                        css += getString(R.string.MaterialOrange);
                        break;
                    case "MaterialGooglePlayGreen":
                        css += getString(R.string.MaterialGPG);
                        break;
                    default:
                        break;
                }

                if (url.contains("lookaside") || url.contains("cdn.fbsbx.com")) {
                    Url = url;
                    RequestStoragePermission();
                }

                // Enable or disable FAB
                if (url.contains("messages") || !preferences.getBoolean("fab_enable", false))
                    mMenuFAB.setVisibility(View.GONE);
                else
                    mMenuFAB.setVisibility(View.VISIBLE);

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

                if (preferences.getBoolean("hide_menu_bar", true))
                    css += "#page{top:-45px}";
                // Hide the status editor on the News Feed if setting is enabled
                if (preferences.getBoolean("hide_editor_newsfeed", true))
                    css += "#mbasic_inline_feed_composer{display:none}";

                // Hide the top story panel in news feed right before the status box
                if (preferences.getBoolean("hide_top_story", true))
                    css += "._59e9._55wr._4g33._400s{display:none}";

                // Hide 'Sponsored' content (ads)
                if (preferences.getBoolean("hide_sponsored", true))
                    css += "article[data-ft*=ei]{display:none}";

                // Hide birthday content from News Feed
                if (preferences.getBoolean("hide_birthdays", true))
                    css += "article#u_1j_4{display:none}article._55wm._5e4e._5fjt{display:none}";

                if (preferences.getBoolean("comments_recently", true))
                    css += "._15ks+._4u3j{display:none}";

                if (sharedFromGallery != null)
                    view.loadUrl("javascript:(function(){try{document.getElementsByClassName(\"_56bz _54k8 _52jh _5j35 _157e\")[0].click()}catch(_){document.getElementsByClassName(\"_50ux\")[0].click()}})()");

                css += "article#u_0_q._d2r{display:none}*{-webkit-tap-highlight-color:transparent;outline:0}";
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int progress) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && showAnimation && progress == 100) {
                    circleReveal();
                    showAnimation = false;
                }
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {

                // Double check that we don't have any existing callbacks
                if (sharedFromGallery != null)
                    filePathCallback.onReceiveValue(new Uri[]{sharedFromGallery});

                mFilePathCallback = filePathCallback;

                // Set up the take picture intent
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    // Create the File where the photo should go
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                        takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                    } catch (IOException ex) {
                        // Error occurred while creating the File
                    }

                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                    } else
                        takePictureIntent = null;
                }
                // Set up the intent to get an existing image
                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("*/*");

                // Set up the intents for the Intent chooser
                Intent[] intentArray;
                if (takePictureIntent != null)
                    intentArray = new Intent[]{takePictureIntent};
                else
                    intentArray = new Intent[0];

                if (sharedFromGallery == null) {
                    Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                    chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                    chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                    startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);
                }
                return true;
            }

            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                openFileChooser(uploadMsg, acceptType);
            }

            // openFileChooser for Android 3.0+
            private void openFileChooser(ValueCallback uploadMsg, String acceptType) {

                // Update message
                if (sharedFromGallery != null)
                    uploadMsg.onReceiveValue(sharedFromGallery);
                else
                    mUploadMessage = uploadMsg;

                File imageStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                if (!imageStorageDir.exists())
                    imageStorageDir.mkdirs();

                // Create camera captured image file path and name
                File file = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");

                mCapturedImageURI = Uri.fromFile(file);

                // Camera capture image intent
                final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);

                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");

                if (sharedFromGallery == null) {
                    Intent chooserIntent = Intent.createChooser(i, "Image Chooser");
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{captureIntent});
                    startActivityForResult(chooserIntent, FILE_CHOOSER_RESULT_CODE);
                }
            }

            private File createImageFile() throws IOException {
                // Create an image file name
                final String imageFileName = "JPEG_" + String.valueOf(System.currentTimeMillis()) + "_";
                File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                final File imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
                return imageFile;
            }

            public void onGeolocationPermissionsShowPrompt(final String origin, final GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                if (view.getUrl().contains("home.php?sk=h_nor"))
                    setTitle(R.string.menu_top_stories);
                else if (title.contains("Facebook"))
                    setTitle(R.string.menu_most_recent);
                else
                    setTitle(title);
            }
        });

        // Add OnClick listener to Profile picture
        ImageView profileImage = navigationView.getHeaderView(0).findViewById(R.id.profile_picture);
        profileImage.setClickable(true);
        profileImage.setOnClickListener(v -> {
            drawer.closeDrawers();
            webView.loadUrl(baseURL + "me");
        });

        mDownloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        if (getIntent() != null)
            UrlIntent(getIntent());
    }

    private void imageLoader(String url, WebView view) {
        startActivity(new Intent(this, Photo.class).putExtra("link", url).putExtra("title", view.getTitle()));
        view.stopLoading();
        view.goBack();
    }

    public void RequestStoragePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
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
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(Url));
                    File downloads_dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (!downloads_dir.exists())
                        if (!downloads_dir.mkdirs())
                            return;

                    File destinationFile = new File(downloads_dir, Uri.parse(Url).getLastPathSegment());
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

        UrlIntent(intent);
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
            deleteCache(this);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        // Thanks to Koras for the tutorial. http://dev.indywidualni.org/2015/02/an-advanced-webview-with-some-cool-features
        if (Build.VERSION.SDK_INT >= 21) {
            if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null)
                return;

            Uri[] results = null;

            // Check that the response is a good one
            if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    // If there is not data, then we may have taken a photo
                    if (mCameraPhotoPath != null) {
                        results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                    }
                } else {
                    String dataString = data.getDataString();
                    if (dataString != null)
                        results = new Uri[]{Uri.parse(dataString)};
                }
            }

            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;
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

    @SuppressWarnings("NewApi")
    private void circleReveal() {
        int radius = (int) Math.hypot(webView.getWidth(), webView.getHeight());
        int x, y;
        if (circleRevealView != null) {
            Point point = getPointOfView(circleRevealView);
            x = point.x;
            y = point.y;
        } else {
            x = 0;
            y = webView.getHeight() / 2;
        }
        Animator anim = ViewAnimationUtils.createCircularReveal(webView, x, y, 0, radius);
        anim.setDuration(300);
        anim.start();
        webView.setVisibility(View.VISIBLE);
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

    private static void deleteCache(Context context) {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        View action_notif = menu.findItem(R.id.action_notifications).getActionView();
        View action_msg = menu.findItem(R.id.action_messages).getActionView();

        ImageView notif = action_notif.findViewById(R.id.badge_icon);
        setBackground(notif);
        notif.setImageDrawable(getResources().getDrawable(R.drawable.ic_notifications));
        notif.setOnClickListener(v -> {
            webView.setVisibility(View.INVISIBLE);
            showAnimation = true;
            circleRevealView = v;

            webView.stopLoading();
            webView.loadUrl(baseURL + "notifications.php");
            setTitle(R.string.nav_notifications);
            Helpers.uncheckRadioMenu(navigationView.getMenu());
            NotificationsJIS.ClearbyId(MainActivity.this, 1);
        });
        ImageView msg = action_msg.findViewById(R.id.badge_icon);
        setBackground(msg);
        msg.setImageDrawable(getResources().getDrawable(R.drawable.ic_message));
        msg.setOnClickListener(v -> {
            webView.setVisibility(View.INVISIBLE);
            showAnimation = true;
            circleRevealView = v;

            webView.stopLoading();
            webView.loadUrl(baseURL + "messages/");
            setTitle(R.string.menu_messages);
            NotificationsJIS.ClearbyId(MainActivity.this, 969);
            Helpers.uncheckRadioMenu(navigationView.getMenu());
        });
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
        setupBaseUrl();

        showAnimation = true;
        circleRevealView = null;
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
                css += "._129- {position:initial}";
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
                css += "#page{top:0}";
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
        txtFormat(friendsRegTv, num, Color.RED, true);
    }

    public void setMrNum(int num) {
        txtFormat(mostRecentTv, num, Color.RED, true);
    }

    private void txtFormat(@Nullable TextView t, int i, int color, boolean bold) {
        if (t != null) {
            t.setText(String.format("%s", i));
            t.setTextColor(color);
            t.setGravity(Gravity.CENTER_VERTICAL);
            if (bold)
                t.setTypeface(null, Typeface.BOLD);
            if (i > 0)
                t.setVisibility(View.VISIBLE);
            else
                t.setVisibility(View.INVISIBLE);
        }
    }

    private void UrlIntent(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
            if (URLUtil.isValidUrl(intent.getStringExtra(Intent.EXTRA_TEXT))) {
                try {
                    webView.loadUrl("https://mbasic.facebook.com/composer/?text=" + URLEncoder.encode(intent.getStringExtra(Intent.EXTRA_TEXT), "utf-8"));
                } catch (UnsupportedEncodingException uee) {
                    uee.printStackTrace();
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
                css += "#mbasic_inline_feed_composer{display:initial}";
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

    @SuppressWarnings("NewApi")
    private void circleReveal(int viewID, final boolean show) {
        final View v = findViewById(viewID);

        int cy = v.getHeight() / 2;

        Animator anim;
        if (show)
            anim = ViewAnimationUtils.createCircularReveal(v, v.getWidth(), cy, 0, v.getWidth());
        else
            anim = ViewAnimationUtils.createCircularReveal(v, v.getWidth(), cy, v.getWidth(), 0);

        anim.setDuration(220);

        // make the view invisible when the animation is done
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!show) {
                    super.onAnimationEnd(animation);
                    v.setVisibility(View.GONE);
                }
            }
        });

        // make the view visible and start the animation
        if (show)
            v.setVisibility(View.VISIBLE);

        anim.start();
    }

    private void setBackground(View btn) {
        TypedValue typedValue = new TypedValue();
        int bg;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            bg = android.R.attr.selectableItemBackgroundBorderless;
        else
            bg = android.R.attr.selectableItemBackground;
        getTheme().resolveAttribute(bg, typedValue, true);
        btn.setBackgroundResource(typedValue.resourceId);
    }
}
