package com.monitoringtool.awarebrowser;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.os.AsyncTask;
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
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class BrowserActivity extends ActionBarActivity {

    public static final String LOG_TAG = "AwareBrows";

    public static final String ACTION_AWARE_CLOSE_BROWSER = "ACTION_AWARE_CLOSE_BROWSER";
    public static final String ACTION_AWARE_READY = "ACTION_AWARE_READY";

    public static final String RESEARCH_WEBSITE = "http://www.mariak.webd.pl/study/";
    public static final boolean MONITORING_DEBUG_FLAG = false;

    public static final String SHARED_PREF_FILE = "mySharedPref";
    public static SharedPreferences mySharedPref;
    public static SharedPreferences.Editor editor;

    public static final String KEY_IS_BROWSER_SERVICE_RUNNING = "KEY_ID_BROWSER_SERVICE_RUNNING";

    private static final String googlePageSearch = "http://www.google.com/search?output=ie&q=";

    private WebView webPageView;
    private LinearLayout browserLayout;
    private EditText etgivenWebSite;
    private MenuItem itemSearch;
    private ImageButton ibBack;

    private String defaultSite = "http://www.google.com/";
    private long LoadTimeSystem = 0;
    private long startTimeSystem = 0;
    private long endTimeSystem = 0;
    private boolean javaScriptStatus = true;

    private boolean loadingFinished = true;
    private boolean redirection = false;
    private boolean loadingFinishedForChrome = true;

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


    @TargetApi(Build.VERSION_CODES.KITKAT)
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        prepareLayout();
        prepareWebPageView();

       // figure out why there is sometimes problem with this
        mySharedPref = getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE);
        editor = mySharedPref.edit();

        displayProgressDialogAboutAware();
        prepareAware();

        IntentFilter filterSetAware = new IntentFilter();
        filterSetAware.addAction(ACTION_AWARE_READY);
        registerReceiver(awareReadyListener, filterSetAware);

        searchForWebPage(defaultSite);

    }

    private void prepareAware() {
        if (!mySharedPref.getBoolean(KEY_IS_BROWSER_SERVICE_RUNNING, false)) {
            if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "RUN BrowserPlugin");
            Intent browserService = new Intent(getApplicationContext(), BrowserPlugin.class);
            getApplicationContext().startService(browserService);
        }
    }


    private void displayProgressDialogAboutAware() {
        if (!isAwareReady && !mySharedPref.getBoolean(KEY_IS_BROWSER_SERVICE_RUNNING, false)) {
            progressDialog = ProgressDialog.show(this, "Aware", this.getResources().getString(R.string.wait_for_aware), true, false);
        }
    }

    private void prepareLayout() {
        browserLayout = (LinearLayout) findViewById(R.id.main_browser_layout);
        etgivenWebSite = (EditText) findViewById(R.id.website_name);
        etgivenWebSite.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean isFocused) {
                if (!isFocused) {
                    InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    keyboard.hideSoftInputFromWindow(browserLayout.getWindowToken(), 0);
                }
                if (isFocused && getWebSiteFromEditText().length() != 0) {
                    etgivenWebSite.setSelection(0, etgivenWebSite.getText().length());
                }
            }
        });

        ibBack = (ImageButton) findViewById(R.id.back);
        prepareToolbar();
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
                    case R.id.action_refresh:
                        webPageView.reload();
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
    private void prepareWebPageView() {
        webPageView = (WebView) findViewById(R.id.webPageView);
        //Enable Javascript and ZOOM
        WebSettings settings = webPageView.getSettings();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
           if(MONITORING_DEBUG_FLAG)  WebView.setWebContentsDebuggingEnabled(true);
        }
        webPageView.setWebChromeClient(new WebChromeClient());
        settings.setBuiltInZoomControls(true);
        settings.setSupportZoom(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setDomStorageEnabled(true);
        webPageView.getSettings().setJavaScriptEnabled(javaScriptStatus);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.menu_browser, menu);

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "BrowserActivty On Resume called");

    }

    public void searchForWebPage(String webSite) {
        webPageView.requestFocus();
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



    private void webViewOnPageFinishOnPageStartMethod(final String webSite){

        webPageView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (!redirection) {
                    loadingFinishedForChrome = true;
                }

                etgivenWebSite.setEnabled(false);
                itemSearch.setEnabled(false);
                if (loadingFinishedForChrome && !redirection) {
                    Log.d(LOG_TAG, "On Progress changed " + newProgress);

                    etgivenWebSite.setText(getResources().getString(R.string.info_loading) + " " + newProgress + "%");
                    if (newProgress == 100) {
                        if (MONITORING_DEBUG_FLAG)
                            Log.d(LOG_TAG, "On Progress changed has reached 100%");
                        itemSearch.setEnabled(true);
                        etgivenWebSite.setText(webPageView.getUrl());
                        etgivenWebSite.setEnabled(true);

                    }

                }
            }
        });

        webPageView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "Redirect..." + view.getUrl());
                if (!loadingFinished)
                    redirection = true;
                loadingFinished = false;
                loadingFinishedForChrome = false;
                webPageView.loadUrl(url);

                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                startTimeSystem = System.currentTimeMillis();
                loadingFinished = false;
                loadingFinishedForChrome = false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
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


        webPageView.loadUrl(webSite);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onUserLeaveHint() {
        finish();
        super.onUserLeaveHint();
    }

    @Override
    protected void onStop() {
        super.onStop();
            if (MONITORING_DEBUG_FLAG)
                Log.d(LOG_TAG, "Browser Activity on Stoped");
        //displayQuitDialog();


    }


private void displayQuitDialog(){
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        finish();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        builder.setMessage("Do you want to end borwsing session?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "BrowserActivty terminated");
        Intent browserClosed = new Intent();
        browserClosed.setAction(ACTION_AWARE_CLOSE_BROWSER);
        sendBroadcast(browserClosed);
        if(MONITORING_DEBUG_FLAG)Log.d(LOG_TAG, "Send ACTION_CLOSE_BROWSER");
        unregisterReceiver(awareReadyListener);
    }
}
