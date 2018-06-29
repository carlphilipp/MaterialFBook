package me.zeeroooo.materialfb.webview

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebView
import me.zeeroooo.materialfb.R
import me.zeeroooo.materialfb.activity.MainActivity
import me.zeeroooo.materialfb.misc.Constant.INPUT_FILE_REQUEST_CODE
import java.io.File
import java.io.IOException

class WebChromeClient(private val activity: MainActivity) : android.webkit.WebChromeClient() {

    override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams): Boolean {

        // Double check that we don't have any existing callbacks
        if (activity.sharedFromGallery != null)
            filePathCallback.onReceiveValue(arrayOf(activity.sharedFromGallery!!))

        activity.filePathCallback = filePathCallback

        // Set up the take picture intent
        var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent!!.resolveActivity(activity.packageManager) != null) {
            // Create the File where the photo should go
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
                takePictureIntent.putExtra("PhotoPath", activity.cameraPhotoPath)
            } catch (ex: IOException) {
                // Error occurred while creating the File
                Log.e(TAG, ex.message, ex)
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                activity.cameraPhotoPath = "file:" + photoFile.absolutePath
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))
            } else
                takePictureIntent = null
        }
        // Set up the intent to get an existing image
        val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
        contentSelectionIntent.type = "*/*"

        // Set up the intents for the Intent chooser
        val intentArray: Array<Intent>
        intentArray = if (takePictureIntent != null) arrayOf(takePictureIntent) else arrayOf()

        if (activity.sharedFromGallery == null) {
            val chooserIntent = Intent(Intent.ACTION_CHOOSER)
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
            activity.startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE)
        }
        return true
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val imageFileName = "JPEG_" + System.currentTimeMillis().toString() + "_"
        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
        callback.invoke(origin, true, false)
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        when {
            view.url.contains("home.php?sk=h_nor") -> activity.setTitle(R.string.menu_top_stories)
            title.contains("Facebook") -> activity.setTitle(R.string.menu_most_recent)
            else -> activity.title = title
        }
    }

    companion object {
        private val TAG = WebChromeClient::class.java.simpleName
    }
}
