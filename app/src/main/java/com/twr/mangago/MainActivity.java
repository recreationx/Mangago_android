package com.twr.mangago;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;


import androidx.appcompat.widget.Toolbar;

import com.google.android.material.color.DynamicColors;

import java.io.InputStream;



public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private RelativeLayout mainContainer;
    Boolean multipage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webview);
        Toolbar toolBar = findViewById(R.id.toolBar);
        setSupportActionBar(toolBar);
        swipeRefresh = findViewById(R.id.swipeContainer);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                webView.reload();
            }
        });
        loadWeb();
    }

    public void loadWeb() {
        mainContainer = findViewById(R.id.mainContainer);
        webView = findViewById(R.id.webview);
        webView.getSettings().setDomStorageEnabled(true);
        webView.loadUrl("https://www.mangago.me/");
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        swipeRefresh.setRefreshing(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
                 public void onPageFinished(WebView view, String url) {
                     Log.d("OPEN", String.valueOf(webView.getProgress()));
                     injectCSS();
                     swipeRefresh.setRefreshing(false);
                     if (webView.getProgress() == 100) {
                         checkUrl(url);
                     }
                     super.onPageFinished(view, url);
                 }
             }
        );
    }

    private void checkUrl(String url) {
        webView.evaluateJavascript("(function() { var element = document.getElementById('reader-nav'); return element.innerHTML; })();",
                s -> {
                    if ( s.equals("null") || s.equals("undefined")) {
                        Log.v("checkURL", s);
                        mainContainer.setFitsSystemWindows(true);
                    } else {
                        webView.goBack();
                        Intent intent = new Intent(this, MangaReaderActivity.class);
                        intent.putExtra("MESSAGE", url);
                        startActivity(intent);

                    }
                });
    }

    private void injectCSS() {
        try {
            InputStream inputStream = getAssets().open("style.css");
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
            String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
            webView.loadUrl("javascript:(function() {" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "var style = document.createElement('style');" +
                    "style.type = 'text/css';" +
                    // Tell the browser to BASE64-decode the string into your script !!!
                    "style.innerHTML = window.atob('" + encoded + "');" +
                    "parent.appendChild(style)" +
                    "})()");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onBackPressed() {

        if (webView.isFocused() && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        MenuItem menuItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setQueryHint("Search mangas");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                webView.loadUrl("https://www.mangago.me/r/l_search/?name=" + query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.register:
                webView.loadUrl("https://www.mangago.me/home/accounts/register/");
                return true;
            case R.id.signin:
                webView.loadUrl("https://www.mangago.me/home/accounts/login/");
                return true;
            case R.id.home:
                webView.loadUrl("https://www.mangago.me/");
                return true;
            case R.id.history:
                webView.loadUrl("https://www.mangago.me/home/history/");
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}







