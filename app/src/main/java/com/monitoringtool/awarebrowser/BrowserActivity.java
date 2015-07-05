package com.monitoringtool.awarebrowser;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;


public class BrowserActivity extends ToolbarActivity {


    public static final String googlePageSearch = "http://www.google.com/search?output=ie&q=";

    private WebView webPageView;
    private LinearLayout browserLayout;
    private EditText etgivenWebSite;
    private MenuItem itemSearch;
    private ImageButton ibBack;

    private String defaultSite = "http://www.google.com";
    private String webSiteToSearch=null;
    private long LoadTimeSystem = 0;
    private long startTimeSystem = 0;
    private long endTimeSystem = 0;
    private boolean javaScriptStatus = true;
    private static final int timesToSearch = 1;
    private int badURLtimes = 0;
    private int pageError = 0;



    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);
        setIsBrowserActivityVisible(true);


        webPageView = (WebView) findViewById(R.id.webPageView);
        browserLayout = (LinearLayout) findViewById(R.id.main_browser_layout);
        etgivenWebSite = (EditText) findViewById(R.id.website_name);
        ibBack = (ImageButton) findViewById(R.id.back);
        super.prepareToolbar();

        if(MONITORING_DEBUG_FLAG) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
        }
        if(mySharedPref.getBoolean(KEY_FIRST_INSTALL, true)) {
            Toast.makeText(getApplicationContext(), "Fist install, the next time you run the application the questionary after session will be triggered!", Toast.LENGTH_LONG).show();
        }
        //Enable Javascript, webView does not allow JS by default
        WebSettings settings = webPageView.getSettings();
        settings.setJavaScriptEnabled(javaScriptStatus);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        itemSearch = menu.findItem(R.id.action_search);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "BrowserActivity On Start called");
        setIsBrowserActivityVisible(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "BrowserActivty On Resume called");

        Intent intent = getIntent();
        webSiteToSearch = intent.getStringExtra(ToolbarActivity.EXTRA_WEB_SITE);

        String webSite;
        if (webSiteToSearch != null) webSite = webSiteToSearch;
        else webSite = defaultSite;

        pageError=0;
        badURLtimes=0;
        searchForWebPage(webSite);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "On new Intent");
        setIntent(intent);
    }

    public void searchForWebPage(String webSite) {
        UrlValidator urlToValidate = new UrlValidator(webSite);
        if(urlToValidate.checkUrl()) {
                webViewOnPageFinishOnPageStartMethod(urlToValidate.getWebSite());

        }else{
            if(badURLtimes < timesToSearch) {
                String webSiteNoSpaces =  repaceSpacesInString(webSite);
                if (MONITORING_DEBUG_FLAG)
                    Log.d(LOG_TAG, "Search in google: " + googlePageSearch + webSiteNoSpaces + " BarURL times: " + String.valueOf(badURLtimes));
                searchForWebPage(googlePageSearch + webSiteNoSpaces);
            }else{
                Toast.makeText(getApplicationContext(), R.string.bad_url, Toast.LENGTH_LONG).show();
            }
            badURLtimes++;
        }

    }

    private String repaceSpacesInString(String webSite) {
        return webSite.replaceAll(" ", "+");
    }

    private class UrlValidator{
        private String webSite;

        public UrlValidator(String webSite) {
            this.webSite = webSite;
        }
        public String getWebSite(){
            return webSite;
        }
        public void setWebSite(String newWebSiteValue){
            webSite = newWebSiteValue;
        }
        public boolean checkUrl() {
           if (Patterns.WEB_URL.matcher(webSite).matches()) {
                if (URLUtil.isHttpsUrl(webSite)) {
                    return true;
                }
                if (URLUtil.isHttpUrl(webSite)) {
                    return true;
                }
                else{
                    setWebSite("http://"+webSite);
                    return true;
                }
            }
            return false;
        }

    }


    private void webViewOnPageFinishOnPageStartMethod(String webSite) {
        webPageView.loadUrl(webSite);
        webPageView.setWebViewClient(new WebViewClient() {
            boolean loadingFinished = true;
            boolean redirection = false;

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (!loadingFinished)
                    redirection = true;
                loadingFinished = false;
                webPageView.loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                startTimeSystem = System.currentTimeMillis();
                loadingFinished = false;

                InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.hideSoftInputFromWindow(browserLayout.getWindowToken(), 0);
                etgivenWebSite.setText(R.string.info_loading);
                etgivenWebSite.setEnabled(false);
                ibBack.setEnabled(false);
                itemSearch.setEnabled(false);

            }


            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!redirection) {
                    loadingFinished = true;
                }
                if (loadingFinished && !redirection) {
                    endTimeSystem = System.currentTimeMillis();
                    LoadTimeSystem = endTimeSystem - startTimeSystem;

                    if(!mySharedPref.getBoolean(KEY_FIRST_INSTALL, true)) {
                        /*Send data to browser provider*/
                        ContentValues plt_data = new ContentValues();
                        plt_data.put(Browser_Provider.Browser_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                        plt_data.put(Browser_Provider.Browser_Data.TIMESTAMP, System.currentTimeMillis());
                        plt_data.put(Browser_Provider.Browser_Data.SESSION_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.SESSION_ID));
                        plt_data.put(Browser_Provider.Browser_Data.WEB_PAGE, webPageView.getUrl());
                        plt_data.put(Browser_Provider.Browser_Data.PAGE_LOAD_TIME, LoadTimeSystem);
                        try {
                            getBaseContext().getContentResolver().insert(Browser_Provider.Browser_Data.CONTENT_URI, plt_data);

                        } catch (SQLiteException e) {
                            if (Aware.DEBUG) Log.d(LOG_TAG, e.getMessage());
                        }

                        sendBroadcast(new Intent(Aware.ACTION_AWARE_CURRENT_CONTEXT));
                    }
                    if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, webPageView.getUrl() + " PLT:"
                            + LoadTimeSystem + "ms");
                    etgivenWebSite.setText("");
                    etgivenWebSite.setEnabled(true);
                    ibBack.setEnabled(true);
                    itemSearch.setEnabled(true);
                } else {
                    redirection = false;
                }

            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "Error loading page: " + failingUrl);
                if (pageError < timesToSearch) {
                    searchForWebPage(googlePageSearch + failingUrl);
                }
                else {
                    Toast.makeText(getApplicationContext(), R.string.bad_connection, Toast.LENGTH_LONG).show();
                }
                pageError++;
            }
        });
    }



    @Override
    protected void onStop() {
        super.onStop();
        setIsBrowserActivityVisible(false);
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "BrowserActivty On Stop called");

    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "BrowserActivty On Destroy called");
    }
}
