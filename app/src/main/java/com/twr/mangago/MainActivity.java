package com.twr.mangago;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.google.android.material.color.DynamicColors;

import java.io.InputStream;



public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private RelativeLayout mainContainer;
    private Toolbar toolBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolBar = (Toolbar) findViewById(R.id.toolBar);
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

    public void loadWeb(){
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
                 injectCSS();
                 super.onPageFinished(view, url);
                 swipeRefresh.setRefreshing(false);
                 showSystemBars();
                 if (checkUrl(url)) {
                     //immersive mode when at the reading page
                     hideSystemBars();
                     mainContainer.setFitsSystemWindows(false);
                 }
                 else{
                     showSystemBars();
                     mainContainer.setFitsSystemWindows(true);
                 }
             }
         }
        );
    }
    private boolean checkUrl(String url) {
        /*Checks if the web link is a reading page*/
        char slash = '/';
        int count = 0;
        for (int i = 0; i < url.length(); i++){
            if (url.charAt(i) == slash) {
                count++;
            }
        }
        return count == 8;
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

   private void hideSystemBars(){
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
           getWindow().setDecorFitsSystemWindows(false);
           WindowInsetsController controller = getWindow().getInsetsController();
           if(controller != null) {
               controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
               controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
           }
           getSupportActionBar().hide();
       }
       else{
           getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                   | View.SYSTEM_UI_FLAG_FULLSCREEN
                   | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                   | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                   | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                   | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
          );}

       }

   private void showSystemBars(){
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
           WindowInsetsController controller = getWindow().getInsetsController();
           if (controller != null) {
               controller.show(WindowInsetsCompat.Type.systemBars());
           }
            getSupportActionBar().show();
       } else{
           getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                   | View.SYSTEM_UI_FLAG_FULLSCREEN
                   | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                   | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                   | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                   | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
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
        }
        return super.onOptionsItemSelected(item);
    }

}

