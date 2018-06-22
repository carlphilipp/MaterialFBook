package me.zeeroooo.materialfb.webview;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import java.io.File;
import java.io.IOException;

import me.zeeroooo.materialfb.R;
import me.zeeroooo.materialfb.activities.MainActivity;

public class WebChromeClient extends android.webkit.WebChromeClient {

    private final MainActivity activity;

    public WebChromeClient(final MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, android.webkit.WebChromeClient.FileChooserParams fileChooserParams) {

        // Double check that we don't have any existing callbacks
        if (activity.getSharedFromGallery() != null)
            filePathCallback.onReceiveValue(new Uri[]{activity.getSharedFromGallery()});

        activity.setFilePathCallback(filePathCallback);

        // Set up the take picture intent
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
                takePictureIntent.putExtra("PhotoPath", activity.getCameraPhotoPath());
            } catch (IOException ex) {
                // Error occurred while creating the File
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                activity.setCameraPhotoPath("file:" + photoFile.getAbsolutePath());
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

        if (activity.getSharedFromGallery() == null) {
            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
            activity.startActivityForResult(chooserIntent, MainActivity.INPUT_FILE_REQUEST_CODE);
        }
        return true;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        final String imageFileName = "JPEG_" + String.valueOf(System.currentTimeMillis()) + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    public void onGeolocationPermissionsShowPrompt(final String origin, final GeolocationPermissions.Callback callback) {
        callback.invoke(origin, true, false);
    }

    @Override
    public void onReceivedTitle(WebView view, String title) {
        if (view.getUrl().contains("home.php?sk=h_nor"))
            activity.setTitle(R.string.menu_top_stories);
        else if (title.contains("Facebook"))
            activity.setTitle(R.string.menu_most_recent);
        else
            activity.setTitle(title);
    }
}
