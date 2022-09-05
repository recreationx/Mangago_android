package com.recreation.mangago

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowInsetsCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.color.DynamicColors
import java.lang.Exception

class MangaReaderActivity : AppCompatActivity() {

    private val webView: WebView by lazy { findViewById(R.id.readerView) }
    private val swipeRefresh: SwipeRefreshLayout by lazy { findViewById(R.id.readerContainer) }
    private val readerBar: Toolbar by lazy { findViewById(R.id.readerBar) }
    private val bottomBar: BottomAppBar by lazy { findViewById(R.id.bottomAppBar) }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manga_reader)
        setSupportActionBar(readerBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        bottomBar.replaceMenu(R.menu.reader_bottom)
        bottomBar.setOnMenuItemClickListener(OnBottomMenuItemClickListener())
        swipeRefresh.setOnRefreshListener { webView.reload() }
        loadWeb()
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun loadWeb() {
        val intent = intent
        val url = intent.getStringExtra("MESSAGE")
        val webViewSettings = webView.settings
        webViewSettings.domStorageEnabled = true
        webViewSettings.javaScriptEnabled = true
        webViewSettings.builtInZoomControls = true
        webViewSettings.displayZoomControls = false
        webView.overScrollMode = View.OVER_SCROLL_NEVER
        webView.webViewClient = ReaderWebViewClient()
        webView.loadUrl(url!!)
        webView.setOnTouchListener(
            WebViewOnTouchListener()
        )
    }

    inner class WebViewOnTouchListener : View.OnTouchListener {
        private var gestureDetector =
            GestureDetector(applicationContext, object : GestureDetector.SimpleOnGestureListener() {
                override fun onLongPress(e: MotionEvent) {
                    if (supportActionBar!!.isShowing) {
                        hideSystemBars()
                    } else {
                        showSystemBars()
                    }
                    return super.onLongPress(e)
                }
            })

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
            gestureDetector.onTouchEvent(motionEvent)
            return false
        }
    }

    inner class ReaderWebViewClient : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                swipeRefresh.isRefreshing = true
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                injectCSS()
                super.onPageFinished(view, url)
                swipeRefresh.isRefreshing = false
            }

            private fun injectCSS() {
                try {
                    val inputStream = assets.open("style.css")
                    val buffer = ByteArray(inputStream.available())
                    inputStream.read(buffer)
                    inputStream.close()
                    val encoded = Base64.encodeToString(buffer, Base64.NO_WRAP)
                    webView.loadUrl(
                        "javascript:(function() {" +
                                "var parent = document.getElementsByTagName('head').item(0);" +
                                "var style = document.createElement('style');" +
                                "style.type = 'text/css';" +
                                "style.innerHTML = window.atob('" + encoded + "');" +
                                "parent.appendChild(style)" +
                                "})()"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }

    private fun showSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
            val controller = window.insetsController
            controller?.show(WindowInsetsCompat.Type.systemBars())
            supportActionBar!!.show()
            bottomBar.performShow()
        }
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            val controller = window.insetsController
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            supportActionBar!!.hide()
            bottomBar.performHide()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.reader_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.multipage -> {
                webView.loadUrl("javascript: document.getElementById('multi_page').click();")
                Toast.makeText(this, "Multipage toggled", Toast.LENGTH_SHORT).show()
                return true
            }
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    inner class OnBottomMenuItemClickListener : Toolbar.OnMenuItemClickListener {
        override fun onMenuItemClick(item: MenuItem?): Boolean {
            when (item?.itemId) {
                R.id.previous -> {
                    return true
                }
                R.id.next -> {
                    return true
                }
            }
            return true
        }
    }
}