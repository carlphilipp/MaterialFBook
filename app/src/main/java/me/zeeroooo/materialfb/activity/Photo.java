package me.zeeroooo.materialfb.activity;

import android.Manifest;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.github.chrisbanes.photoview.OnPhotoTapListener;
import com.github.chrisbanes.photoview.PhotoView;
import me.zeeroooo.materialfb.R;
import me.zeeroooo.materialfb.ui.CookingAToast;

import java.io.File;

public class Photo extends AppCompatActivity implements OnPhotoTapListener {

    private PhotoView imageView;
    private DownloadManager downloadManager;
    private ProgressBar progressBar;
    private Target<Bitmap> shareTarget;
    private boolean download = false, countdown = false;
    private int share = 0;
    private TextView imageTitle;
    private View topGradient;
    private Toolbar toolbar;
    private String imageUrl;
    private WebView webView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_photo);

        imageView = findViewById(R.id.photo);
        topGradient = findViewById(R.id.photoViewerTopGradient);
        toolbar = findViewById(R.id.toolbar_ph);
        imageTitle = findViewById(R.id.photo_title);
        progressBar = findViewById(android.R.id.progress);
        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        webView = new WebView(this);

        imageView.setOnPhotoTapListener(this);
        imageTitle.setText(getIntent().getStringExtra("title"));

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        webView.getSettings().setBlockNetworkImage(true);
        webView.getSettings().setAppCacheEnabled(false);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        webView.loadUrl(getIntent().getStringExtra("link"));
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                imageUrl = url;
                load();
            }
        });

    }

    @Override
    public void onWindowFocusChanged(final boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LOW_PROFILE
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    public void onPhotoTap(final ImageView view, final float x, final float y) {
        setVisibility(View.VISIBLE, android.R.anim.fade_in);
        setCountDown();
    }

    private void load() {
        Glide.with(this)
                .load(imageUrl)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        setCountDown();
                        return false;
                    }
                })
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE))
                .into(imageView);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.photo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.download_image:
                download = true;
                RequestStoragePermission();
                break;
            case R.id.share_image:
                share = 1;
                RequestStoragePermission();
                break;
            case R.id.copy_url_image:
                final ClipboardManager clipboard = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
                final ClipData clip = ClipData.newUri(this.getContentResolver(), "", Uri.parse(imageUrl));
                if (clipboard != null)
                    clipboard.setPrimaryClip(clip);
                CookingAToast.cooking(Photo.this, getString(R.string.content_copy_link_done), Color.WHITE, Color.parseColor("#00C851"), R.drawable.ic_copy_url, true).show();
                break;
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return false;
    }

    private void shareImage() {
        shareTarget = new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(final Bitmap bitmap, Transition<? super Bitmap> transition) {
                String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, Uri.parse(imageUrl).getLastPathSegment(), null);
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/*");
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(path));
                startActivity(Intent.createChooser(shareIntent, getString(R.string.context_share_image)));
                CookingAToast.cooking(Photo.this, getString(R.string.context_share_image_progress), Color.WHITE, Color.parseColor("#00C851"), R.drawable.ic_share, false).show();
            }
        };
        Glide.with(Photo.this).asBitmap().load(imageUrl).apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE)).into(shareTarget);
        share = 2;
    }

    private void RequestStoragePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String permissions[], @NonNull final int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (share == 1)
                        shareImage();
                    else if (download) {
                        // Save the image
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(imageUrl));

                        // Set the download directory
                        File downloads_dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/MaterialFBook");
                        if (!downloads_dir.exists())
                            downloads_dir.mkdir();
                        File destinationFile = new File(downloads_dir, Uri.parse(imageUrl).getLastPathSegment());
                        request.setDestinationUri(Uri.fromFile(destinationFile));

                        // Make notification stay after download
                        request.setVisibleInDownloadsUi(true);
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                        // Start the download
                        downloadManager.enqueue(request);

                        CookingAToast.cooking(this, getString(R.string.downloaded), Color.WHITE, Color.parseColor("#00C851"), R.drawable.ic_download, false).show();
                        download = false;
                    }
                } else
                    CookingAToast.cooking(this, getString(R.string.permission_denied), Color.WHITE, Color.parseColor("#ff4444"), R.drawable.ic_error, true).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!(share == 2)) {
            if (shareTarget != null)
                Glide.with(Photo.this).clear(shareTarget);
            if (imageView != null)
                imageView.setImageDrawable(null);
        }
        webView.clearCache(true);
        webView.clearHistory();
        webView.removeAllViews();
        webView.destroy();
    }

    public void setVisibility(final int visibility, final int animation) {
        Animation a = AnimationUtils.loadAnimation(this, animation);

        topGradient.startAnimation(a);
        toolbar.startAnimation(a);
        imageTitle.startAnimation(a);

        topGradient.setVisibility(visibility);
        toolbar.setVisibility(visibility);
        imageTitle.setVisibility(visibility);
    }

    private void setCountDown() {
        CountDownTimer countDownTimer = new CountDownTimer(5000, 1000) {
            public void onTick(long millisUntilFinished) {
                countdown = true;
            }

            @Override
            public void onFinish() {
                setVisibility(View.INVISIBLE, android.R.anim.fade_out);
                countdown = false;
            }
        };
        if (!countdown)
            countDownTimer.start();
        else
            countDownTimer.cancel();
    }
}