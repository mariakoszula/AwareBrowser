package com.monitoringtool.awarebrowser;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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


public class BrowserActivity extends ActionBarActivity {

    public static final String LOG_TAG = "AwareBrows";

    public static final String ACTION_AWARE_CLOSE_BROWSER = "ACTION_AWARE_CLOSE_BROWSER";
    public static final String ACTION_AWARE_READY = "ACTION_AWARE_READY";

    public static final String RESEARCH_WEBSITE = "http://www.mariak.webd.pl/study/";
    public static final boolean MONITORING_DEBUG_FLAG = true;

    public static final String SHARED_PREF_FILE = "mySharedPref";
    public static SharedPreferences mySharedPref;
    public static SharedPreferences.Editor editor;

    public static final String KEY_IS_BROWSER_SERVICE_RUNNING = "KEY_ID_BROWSER_SERVICE_RUNNING";
    public static final String KEY_FIRST_INSTALL = "KEY_FIRST_INSTALL";

    private static final String googlePageSearch = "http://www.google.com/search?output=ie&q=";

    private WebView webPageView;
    private LinearLayout browserLayout;
    private EditText etgivenWebSite;
    private MenuItem itemSearch;
    private ImageButton ibBack;

    private String defaultSite = "http://www.google.com";
    private long LoadTimeSystem = 0;
    private long startTimeSystem = 0;
    private long endTimeSystem = 0;
    private boolean javaScriptStatus = true;


    private boolean isAwareReady = false;
    private ProgressDialog progressDialog;
    private final BroadcastReceiver awareReadyListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MONITORING_DEBUG_FLAG)
                Log.d(LOG_TAG, "Received awareReady broadcast");
            isAwareReady = true;
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }
    };


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);
        //setIsBrowserActivityVisible(true);

        browserLayout = (LinearLayout) findViewById(R.id.main_browser_layout);
        etgivenWebSite = (EditText) findViewById(R.id.website_name);
        ibBack = (ImageButton) findViewById(R.id.back);
        webPageView = (WebView) findViewById(R.id.webPageView);
        prepareToolbar();


        if (!isAwareReady) {
            progressDialog = ProgressDialog.show(this, "Aware", this.getResources().getString(R.string.wait_for_aware), true, false);
        }

        //Prapare SharedPreferences
        mySharedPref = getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE);
        editor = mySharedPref.edit();
        if (mySharedPref.getBoolean(KEY_FIRST_INSTALL, true)) {
            if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "First install do not run ESM yet");

            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.first_install), Toast.LENGTH_LONG).show();
        }

        //Enable Javascript and ZOOM
        WebSettings settings = webPageView.getSettings();
        settings.setJavaScriptEnabled(javaScriptStatus);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        settings.setBuiltInZoomControls(true);
        settings.setSupportZoom(true);


        if (!mySharedPref.getBoolean(KEY_IS_BROWSER_SERVICE_RUNNING, false)) {
            if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "RUN BrowserPlugin");
            Intent browserService = new Intent(getApplicationContext(), BrowserPlugin.class);
            getApplicationContext().startService(browserService);
        }

        IntentFilter filterSetAware = new IntentFilter();
        filterSetAware.addAction(ACTION_AWARE_READY);
        registerReceiver(awareReadyListener, filterSetAware);

        searchForWebPage(defaultSite);

    }


    public void prepareToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.browser_toolbar);
        ibBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        setSupportActionBar(toolbar);
        toolbar.inflateMenu(R.menu.menu_browser);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_about:
                        if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "-----action_about-----");
                        searchForWebPage(RESEARCH_WEBSITE);
                        return true;
                    case R.id.action_search:
                        if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "-----action_search-----");
                        searchForWebPage(getWebSiteFromEditText());
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    private String getWebSiteFromEditText() {
        return etgivenWebSite.getText().toString();
    }

    @Override
    public void onBackPressed() {
        if (webPageView.canGoBack()) {
            webPageView.goBack();
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.menu_browser, menu);

        //Search something using EnterKey
        etgivenWebSite.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_ENTER:
                            searchForWebPage(getWebSiteFromEditText());
                            return true;
                    }

                }
                return false;
            }
        });
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
        if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "BrowserActivity On Start called");
       // setIsBrowserActivityVisible(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "BrowserActivty On Resume called");

       // Intent intent = getIntent();
       // webSiteToSearch = intent.getStringExtra(EXTRA_WEB_SITE);

    }


   /* @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "On new Intent");
        setIntent(intent);
    }*/


    public void searchForWebPage(String webSite) {
        UrlValidator urlToValidate = new UrlValidator(webSite);
        if (urlToValidate.checkUrl()) {
            webViewOnPageFinishOnPageStartMethod(urlToValidate.getWebSite());
        } else {
            String webSiteNoSpaces = repaceSpacesInString(webSite);
            webViewOnPageFinishOnPageStartMethod(googlePageSearch + webSiteNoSpaces);
        }
    }

    private String repaceSpacesInString(String webSite) {
        return webSite.replaceAll(" ", "+");
    }

    private class UrlValidator {
        private String webSite;

        public UrlValidator(String webSite) {
            this.webSite = webSite;
        }

        public String getWebSite() {
            return webSite;
        }

        public void setWebSite(String newWebSiteValue) {
            webSite = newWebSiteValue;
        }

        public boolean checkUrl() {
            if (Patterns.WEB_URL.matcher(webSite).matches()) {
                if (URLUtil.isHttpsUrl(webSite)) {
                    return true;
                }
                if (URLUtil.isHttpUrl(webSite)) {
                    return true;
                } else {
                    setWebSite("http://" + webSite);
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
                Toast.makeText(getApplicationContext(), R.string.bad_connection, Toast.LENGTH_LONG).show();

            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStop() {
        super.onStop();
       // setIsBrowserActivityVisible(false);
       // if (!isBrowserActivityVisible()) {
            if (MONITORING_DEBUG_FLAG)
                Log.d(LOG_TAG, "Browser finish() when InstructionsActivityNotVisible and browserActivity not visible");

        if(MONITORING_DEBUG_FLAG)Log.d(LOG_TAG, "Send ACTION_CLOSE_BROWSER");
        Intent browserClosed = new Intent();
        browserClosed.setAction(ACTION_AWARE_CLOSE_BROWSER);
        sendBroadcast(browserClosed);

        finish();
        //}
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "BrowserActivty terminated");
        unregisterReceiver(awareReadyListener);
    }
}
