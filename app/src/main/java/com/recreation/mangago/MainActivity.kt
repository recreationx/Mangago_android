package com.recreation.mangago

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RelativeLayout
import androidx.appcompat.widget.SearchView
import androidx.core.view.WindowInsetsCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.color.DynamicColors
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    private val webView: WebView by lazy { findViewById(R.id.webView) }
    val swipeRefresh: SwipeRefreshLayout by lazy { findViewById(R.id.swipeContainer) }
    val mainContainer: RelativeLayout by lazy { findViewById(R.id.mainContainer) }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolBar))
        swipeRefresh.setOnRefreshListener { webView.reload() }
        loadWeb()
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun loadWeb() {
        val webViewSettings = webView.settings
        webViewSettings.domStorageEnabled = true
        webViewSettings.javaScriptEnabled = true
        webViewSettings.builtInZoomControls = true
        webViewSettings.displayZoomControls = false
        webView.overScrollMode = View.OVER_SCROLL_NEVER
        webView.webViewClient = CustomWebViewClient()
        webView.loadUrl("https://www.mangago.me/")
    }

    inner class CustomWebViewClient : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            swipeRefresh.isRefreshing = true
            super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            injectCSS()
            super.onPageFinished(view, url)
            swipeRefresh.isRefreshing = false
            checkURL(url!!)
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

    private fun checkURL(url: String) {
        var count = 0
        for (char in url) {
            if (char == '/') {
                count++
            }
        }
        if (webView.progress == 100) {
            if (count == 8) {
                Log.v("READER", "COUNT")
                startReader(url)
            } else {
                webView.evaluateJavascript(
                    "(function() { var element = document.getElementById('reader-nav'); return element.innerHTML; })();"
                ) { s: String ->
                    if (s == "null" || s == "undefined") {
                        Log.v("NULL", "NULL")
                    } else {
                        startReader(url)
                        Log.v("READER", "JS")
                    }
                }
            }
        }
    }

    private fun startReader(url: String) {
        webView.goBack()
        val intent = Intent(this, MangaReaderActivity::class.java)
        intent.putExtra("MESSAGE", url)
        startActivity(intent)
    }

    override fun onBackPressed() {
        if (webView.isFocused && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        val menuItem = menu.findItem(R.id.search)
        val searchView = menuItem.actionView as SearchView
        searchView.queryHint = "Search mangas"
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                webView.loadUrl("https://www.mangago.me/r/l_search/?name=$query")
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.register -> {
                webView.loadUrl("https://www.mangago.me/home/accounts/register/")
                return true
            }
            R.id.signin -> {
                webView.loadUrl("https://www.mangago.me/home/accounts/login/")
                return true
            }
            R.id.home -> {
                webView.loadUrl("https://www.mangago.me/")
                return true
            }
            R.id.history -> {
                webView.loadUrl("https://www.mangago.me/home/history/")
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}