package com.recreation.mangago

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.util.Log
import android.view.*
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.color.DynamicColors
import com.google.android.material.navigation.NavigationView
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements


class MangaReaderActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val webView: WebView by lazy { findViewById(R.id.readerView) }
    private val swipeRefresh: SwipeRefreshLayout by lazy { findViewById(R.id.readerContainer) }
    private val readerBar: Toolbar by lazy { findViewById(R.id.readerBar) }
    private val bottomBar: BottomAppBar by lazy { findViewById(R.id.bottomAppBar) }
    private val drawerLayout: DrawerLayout by lazy {findViewById(R.id.drawerLayout) }
    val navigationView: NavigationView by lazy { findViewById(R.id.navigationView) }
    val url: String = "";
    var nextChapter: String = "";
    var previousChapter: String = "";
    val html: String = "";
    var loadedChapters: Boolean = false;

    val chapterArray = ArrayList<String>();
    val chapterMap = HashMap<String, String>();
    val mHandler: Handler = Handler();
    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manga_reader)
        setSupportActionBar(readerBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        bottomBar.replaceMenu(R.menu.reader_bottom)
        bottomBar.setOnMenuItemClickListener(OnBottomMenuItemClickListener())
        swipeRefresh.setOnRefreshListener { webView.reload() }
        navigationView.setNavigationItemSelectedListener(this)
        val drawerToggle = ActionBarDrawerToggle(
            this, drawerLayout, bottomBar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(drawerToggle)
        drawerLayout.closeDrawers()
        drawerToggle.syncState()
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
        webView.addJavascriptInterface(MyJavaScriptInterface(), "HtmlHandler")
        webView.webViewClient = ReaderWebViewClient()
        webView.loadUrl(url!!)
        webView.setOnTouchListener(
            WebViewOnTouchListener()
        )
    }

    inner class MyJavaScriptInterface {
        @JavascriptInterface
        fun handleHtml(html: String?) {
            mHandler.post(Runnable {
                val doc: Document = Jsoup.parse(html!!)
                nextChapter =
                    "https://www.mangago.me" + doc.select("a[class='next_page']").first()!!.attr("href")
                previousChapter =
                    "https://www.mangago.me" + doc.select("a[class='prev_page']").first()!!.attr("href")
                val currentChapter: Element? =
                    doc.select("a[class='btn btn-primary dropdown-toggle chapter btn-inverse']").first()
                val allChapters: Element? = doc.getElementsByClass("dropdown-menu chapter").first()
                val chapters: Elements = allChapters!!.select("a[href]")
                if (!loadedChapters) {
                    generateChapterList(chapters)
                    loadedChapters = true
                }
            })
        }
    }

    fun generateChapterList(chapters: Elements) {
        for (chapter in chapters) {
            val link = "https://www.mangago.me" + chapter.attr("href")
            chapterArray.add(chapter.ownText())
            chapterMap[chapter.ownText()] = link
        }
        Log.v("DDDDD", chapterArray.toString())
        Log.v("DDDDD", chapterMap.toString())
        val menu = navigationView.menu
        for (chapter in chapterArray) {
            menu.add(chapter)
        }
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
                webView.loadUrl("javascript:window.HtmlHandler.handleHtml" +
                        "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        webView.loadUrl(chapterMap[item.title].toString())
        drawerLayout.closeDrawers()
        return true
    }

}


