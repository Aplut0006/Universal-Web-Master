package aplut.myfirstkotlinapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {

    private lateinit var mContext: Context
    internal var mLoaded = false

    internal var URL = "https://www.google.com/"

    private var mCameraPhotoPath: String? = null
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    internal var doubleBackToExitPressedOnce = false

    private lateinit var btnTryAgain: Button
    private lateinit var mWebView: WebView
    private lateinit var progressBar: ProgressBar
    private var splash: View? = null
    private lateinit var layoutSplash: RelativeLayout
    private lateinit var layoutWebview: RelativeLayout
    private lateinit var layoutNoInternet: RelativeLayout

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        mContext = this
        mWebView = findViewById<View>(R.id.webview) as WebView
        progressBar = findViewById<View>(R.id.progressBar) as ProgressBar
        btnTryAgain = findViewById<View>(R.id.btn_try_again) as Button
        splash = findViewById(R.id.view_splash) as View
        layoutWebview = findViewById<View>(R.id.layout_webView) as RelativeLayout
        layoutNoInternet = findViewById<View>(R.id.layout_no_internet) as RelativeLayout
        layoutSplash = findViewById<View>(R.id.layout_splash) as RelativeLayout

        requestForWebview()

        btnTryAgain.setOnClickListener {
            mWebView.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            layoutSplash.visibility = View.VISIBLE
            layoutNoInternet.visibility = View.GONE
            requestForWebview()
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun requestForWebview() {

        if (!mLoaded) {
            requestWebView()
            Handler().postDelayed({
                mWebView.visibility = View.VISIBLE
            }, 3000)

        } else {
            mWebView.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
            layoutSplash.visibility = View.GONE
            layoutNoInternet.visibility = View.GONE
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun requestWebView() {
        if (internetCheck(mContext)) {
            mWebView.visibility = View.VISIBLE
            layoutNoInternet.visibility = View.GONE
            mWebView.loadUrl(URL)
        } else {
            progressBar.visibility = View.GONE
            mWebView.visibility = View.GONE
            layoutSplash.visibility = View.GONE
            layoutNoInternet.visibility = View.VISIBLE

            return
        }
        mWebView.isFocusable = true
        mWebView.isFocusableInTouchMode = true
        mWebView.settings.javaScriptEnabled = true
        mWebView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        mWebView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        mWebView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        mWebView.settings.databaseEnabled = true
        mWebView.settings.domStorageEnabled = true
        mWebView.settings.setAppCacheEnabled(true)
        mWebView.settings.setSupportMultipleWindows(false)
        mWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {

                Log.d(TAG, "URL: " + url!!)
                if (internetCheck(mContext)) {
                    view.loadUrl(url);
                } else {
                    progressBar.visibility = View.GONE
                    mWebView.visibility = View.GONE
                    layoutSplash.visibility = View.GONE
                    layoutNoInternet.visibility = View.VISIBLE
                }
                return true
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (progressBar.visibility == View.GONE) {
                    progressBar.visibility = View.VISIBLE
                }
            }

            override fun onLoadResource(view: WebView, url: String) {
                super.onLoadResource(view, url)
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                mLoaded = true
                if (progressBar.visibility == View.VISIBLE)
                    progressBar.visibility = View.GONE
                Handler().postDelayed({
                    layoutSplash.visibility = View.GONE
                }, 2000)
            }
        }

        mWebView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                    webView: WebView, filePathCallback: ValueCallback<Array<Uri>>,
                    fileChooserParams: WebChromeClient.FileChooserParams): Boolean {
                if (mFilePathCallback != null) {
                    mFilePathCallback!!.onReceiveValue(null)
                }
                mFilePathCallback = filePathCallback

                var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (takePictureIntent!!.resolveActivity(this@MainActivity.packageManager) != null) {
                    var photoFile: File? = null
                    try {
                        photoFile = createImageFile()
                        takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath)
                    } catch (ex: IOException) {
                        Log.e(TAG, "Unable to create Image File", ex)
                    }
                    if (photoFile != null) {
                        mCameraPhotoPath = "file:" + photoFile.absolutePath
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                Uri.fromFile(photoFile))
                    } else {
                        takePictureIntent = null
                    }
                }

                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = "image/*"

                val intentArray: Array<Intent?>
                if (takePictureIntent != null) {
                    intentArray = arrayOf(takePictureIntent)
                } else {
                    intentArray = arrayOfNulls(0)
                }

                val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
                startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE)

                return true
            }
        }

    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        )
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }

        var results: Array<Uri>? = null
        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                if (mCameraPhotoPath != null) {
                    results = arrayOf(Uri.parse(mCameraPhotoPath))
                }
            } else {
                val dataString = data.dataString
                if (dataString != null) {
                    results = arrayOf(Uri.parse(dataString))
                }
            }
        }

        mFilePathCallback!!.onReceiveValue(results)
        mFilePathCallback = null
        return
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && mWebView.canGoBack()) {
            mWebView.goBack()
            return true
        }

        if (doubleBackToExitPressedOnce) {
            return super.onKeyDown(keyCode, event)
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show()

        Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
        return true
    }

    companion object {
        internal var TAG = "---MainActivity"
        val INPUT_FILE_REQUEST_CODE = 1

        fun internetCheck(context: Context): Boolean {
            var available = false
            val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (connectivity != null) {
                val networkInfo = connectivity.allNetworkInfo
                if (networkInfo != null) {
                    for (i in networkInfo.indices) {
                        if (networkInfo[i].state == NetworkInfo.State.CONNECTED) {
                            available = true
                            break
                        }
                    }
                }
            }
            return available
        }
    }
}