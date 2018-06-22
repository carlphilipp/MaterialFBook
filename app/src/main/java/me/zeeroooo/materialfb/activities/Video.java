package me.zeeroooo.materialfb.activities;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import me.zeeroooo.materialfb.R;
import me.zeeroooo.materialfb.ui.CookingAToast;

public class Video extends AppCompatActivity {

    private VideoView mVideoView;
    private int position = 0;
    private DownloadManager mDownloadManager;
    private RelativeLayout mButtonsHeader;
    private SeekBar mSeekbar;
    private String url;
    private TextView mElapsedTime, mRemainingTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        url = getIntent().getStringExtra("video_url");

        mVideoView = findViewById(R.id.video_view);
        mButtonsHeader = findViewById(R.id.buttons_header);
        mSeekbar = findViewById(R.id.progress);
        mDownloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        mElapsedTime = findViewById(R.id.elapsed_time);
        mRemainingTime = findViewById(R.id.remaining_time);

        mSeekbar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        mSeekbar.getThumb().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);

        mVideoView.setVideoURI(Uri.parse(url));

        mVideoView.requestFocus();
        mVideoView.setOnPreparedListener(mediaPlayer -> {
            mVideoView.seekTo(position);
            mSeekbar.setMax(mVideoView.getDuration());
            mSeekbar.postDelayed(Update, 1000);
            mElapsedTime.postDelayed(Update, 1000);
            mRemainingTime.postDelayed(Update, 1000);
            setVisibility(View.GONE, android.R.anim.fade_out);
            if (position == 0)
                mVideoView.start();
        });

        // Buttons
        final ImageButton pause = findViewById(R.id.pauseplay_btn);
        setBackground(pause);
        pause.setOnClickListener(v -> {
            if (mVideoView.isPlaying()) {
                mVideoView.pause();
                ((ImageButton) v).setImageResource(android.R.drawable.ic_media_play);
            } else {
                mVideoView.start();
                ((ImageButton) v).setImageResource(android.R.drawable.ic_media_pause);
            }
        });

        final ImageButton previous = findViewById(R.id.previous_btn);
        setBackground(previous);
        previous.setOnClickListener(v -> {
            mVideoView.seekTo(0);
            mSeekbar.setProgress(0);
        });

        final ImageButton download = findViewById(R.id.download_btn);
        setBackground(download);
        download.setOnClickListener(v -> RequestStoragePermission());

        mVideoView.setOnTouchListener((v, event) -> {
            setCountDown();
            setVisibility(View.VISIBLE, android.R.anim.fade_in);
            return false;
        });

        final ImageButton share = findViewById(R.id.share_btn);
        setBackground(share);
        share.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, url);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.context_share_link)));
        });

        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    mVideoView.seekTo(progress);
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
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

    private final Runnable Update = new Runnable() {
        @Override
        public void run() {
            if (mSeekbar != null) {
                mSeekbar.setProgress(mVideoView.getCurrentPosition());
            }
            if (mVideoView.isPlaying()) {
                mSeekbar.postDelayed(Update, 1000);
                mElapsedTime.setText(Time(mVideoView.getCurrentPosition()));
                mRemainingTime.setText(Time(mVideoView.getDuration() - mVideoView.getCurrentPosition()));
            }
        }
    };

    private String Time(long ms) {
        return String.format(Locale.getDefault(), "%d:%d", TimeUnit.MILLISECONDS.toMinutes(ms), TimeUnit.MILLISECONDS.toSeconds(ms) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((ms))));
    }

    private void RequestStoragePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

                    // Set the download directory
                    File downloads_dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES + "/MaterialFBook");
                    if (!downloads_dir.exists())
                        downloads_dir.mkdir();
                    File destinationFile = new File(downloads_dir, Uri.parse(url).getLastPathSegment());
                    request.setDestinationUri(Uri.fromFile(destinationFile));

                    // Make notification stay after download
                    request.setVisibleInDownloadsUi(true);
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                    // Start the download
                    mDownloadManager.enqueue(request);

                    CookingAToast.cooking(this, getString(R.string.downloaded), Color.WHITE, Color.parseColor("#00C851"), R.drawable.ic_download, false).show();
                } else
                    CookingAToast.cooking(this, getString(R.string.permission_denied), Color.WHITE, Color.parseColor("#ff4444"), R.drawable.ic_error, true).show();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        position = mVideoView.getCurrentPosition();
        mVideoView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mVideoView.seekTo(position);
        mVideoView.start();
    }

    private void setCountDown() {
        new CountDownTimer(5000, 1000) {

            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                setVisibility(View.INVISIBLE, android.R.anim.fade_out);
            }
        }.start();
    }

    public void setVisibility(int visibility, int animation) {
        Animation a = AnimationUtils.loadAnimation(this, animation);

        mButtonsHeader.startAnimation(a);
        mButtonsHeader.setVisibility(visibility);
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
