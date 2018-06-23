package me.zeeroooo.materialfb.webview;

import android.webkit.JavascriptInterface;
import me.zeeroooo.materialfb.activity.MainActivity;

@SuppressWarnings("unused")
public class JavaScriptInterfaces {
    private final MainActivity activity;

    // Instantiate the interface and set the context
    public JavaScriptInterfaces(MainActivity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void getNums(final String notifications, final String messages, final String requests, final String feed) {
        final int notifications_int = Helpers.isInteger(notifications) ? Integer.parseInt(notifications) : 0;
        final int messages_int = Helpers.isInteger(messages) ? Integer.parseInt(messages) : 0;
        final int requests_int = Helpers.isInteger(requests) ? Integer.parseInt(requests) : 0;
        final int mr_int = Helpers.isInteger(feed) ? Integer.parseInt(feed) : 0;
        activity.runOnUiThread(() -> {
            activity.setRequestsNum(requests_int);
            activity.setMrNum(mr_int);
        });
    }
}
