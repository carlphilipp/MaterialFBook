package me.zeeroooo.materialfb.listener;

import android.content.Intent;
import android.view.View;
import me.zeeroooo.materialfb.R;
import me.zeeroooo.materialfb.activity.MainActivity;

public class FabOnClickListener implements View.OnClickListener {

    private final MainActivity activity;

    public FabOnClickListener(final MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onClick(final View view) {
        final String baseURL = activity.getBaseURL();
        switch (view.getId()) {
            case R.id.textFab:
                activity.getWebView().loadUrl("javascript:(function(){try{document.querySelector('button[name=\"view_overview\"]').click()}catch(_){window.location.href=\"" + baseURL + "?pageload=composer\"}})()");
                activity.getSwipeView().setEnabled(false);
                break;
            case R.id.photoFab:
                activity.getWebView().loadUrl("javascript:(function(){try{document.querySelector('button[name=\"view_photo\"]').click()}catch(_){window.location.href=\"" + baseURL + "?pageload=composer_photo\"}})()");
                activity.getSwipeView().setEnabled(false);
                break;
            case R.id.checkinFab:
                activity.getWebView().loadUrl("javascript:(function(){try{document.querySelector('button[name=\"view_location\"]').click()}catch(_){window.location.href=\"" + baseURL + "?pageload=composer_checkin\"}})()");
                activity.getSwipeView().setEnabled(false);
                break;
            case R.id.topFab:
                activity.getWebView().scrollTo(0, 0);
                break;
            case R.id.shareFab:
                final Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, activity.getWebView().getUrl());
                activity.startActivity(Intent.createChooser(shareIntent, activity.getString(R.string.context_share_link)));
                break;
            default:
                break;
        }
        activity.getFloatingActionMenu().close(true);
    }
}
