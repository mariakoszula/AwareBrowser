package com.monitoringtool.awarebrowser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.providers.Accelerometer_Provider;
import com.aware.providers.Aware_Provider;

import junit.framework.Assert;

import org.apache.http.client.utils.URIUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.lang.Object;


public class BrowserActivity extends ActionBarActivity {

    public static final String LOG_TAG = "WebViewLoadingTime";
    public static final String EXTRA_WEB_SITE = "com.monitoringtool.awarebrowser.WEB_SITE";
    public static final String RESEARCH_WEBSITE = "http://www.mariak.webd.pl/study/";
    public static final boolean MONITORING_DEBUG_FLAG = true;
    public static final String googlePageSearch = "http://www.google.com/search?output=ie&q=";

    private boolean isBrowserActivityVisible=false;
    private boolean isInstructionsActivityVisible=false;


    private WebView webPageView;
    private LinearLayout browserLayout;
    private EditText etgivenWebSite;

    private String defaultSite = "http://www.google.com";
    private String webSiteToSearch=null;
    private long LoadTime = 0;
    private long LoadTimeSystem = 0;
    private long startTime = 0;
    private long startTimeSystem = 0;
    private long startTimeHttpURL = 0;
    private long endTime = 0;
    private long endTimeHttpURL = 0;
    private long endTimeSystem = 0;


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "BrowserActivty On create called");
        setIsBrowserActivityVisible(true);
        webPageView = (WebView) findViewById(R.id.webPageView);
        etgivenWebSite = (EditText) findViewById(R.id.website_name);
        browserLayout = (LinearLayout) findViewById(R.id.main_browser_layout);

        //Enable Javascript, webView does not allow JS by default
        WebSettings settings = webPageView.getSettings();
        settings.setJavaScriptEnabled(true);
        prepareToolbar();
       // Log.d(LOG_TAG, String.valueOf(Build.VERSION.SDK_INT));
    }
    @Override
    protected void onStart() {
        super.onStart();
        setIsBrowserActivityVisible(true);
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "On Start called");

        Intent intent = getIntent();
        webSiteToSearch = intent.getStringExtra(BrowserActivity.EXTRA_WEB_SITE);

        String webSite;
        if (webSiteToSearch != null) webSite = webSiteToSearch;
        else webSite = defaultSite;

        startSensors();

        searchForWebPage(webSite);

    }

    public void setIsBrowserActivityVisible(boolean isBrowserActivityVisible) {
        this.isBrowserActivityVisible = isBrowserActivityVisible;
    }

    public void setIsInstructionsActivityVisible(boolean isInstructionsActivityVisible) {
        this.isInstructionsActivityVisible = isInstructionsActivityVisible;
    }

    public void prepareToolbar(){
        final Toolbar toolbar = (Toolbar) findViewById(R.id.browser_toolbar);
        setSupportActionBar(toolbar);


        toolbar.inflateMenu(R.menu.menu_browser);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {


                String givenWebSite = null;
                switch (item.getItemId()) {
                    case R.id.action_about:
                        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "action_about");
                            givenWebSite = RESEARCH_WEBSITE;
                            if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, givenWebSite);
                            return runSearch(givenWebSite);
                    case R.id.action_search:
                        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "action_search");
                        givenWebSite = getWebSiteFromEditText();
                        Log.d(LOG_TAG, givenWebSite);
                        return runSearch(givenWebSite);
                    case R.id.action_instruction:
                        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "action_insturction");
                        if(!isInstructionsActivityVisible) {
                            Intent instructions = new Intent(getApplicationContext(), InstructionsActivity.class);
                            startActivity(instructions);
                            return true;
                        }
                    default:
                        return false;
                }
            }
        });
    }
    private boolean runSearch(String webSite){
        if (isInstructionsActivityVisible) {
            Intent browser = new Intent(getBaseContext(), BrowserActivity.class);
            browser.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            browser.putExtra(EXTRA_WEB_SITE, webSite);
            Log.d(LOG_TAG, "run Search" + webSite);
            startActivity(browser);
        } else {
            searchForWebPage(webSite);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.menu_browser, menu);
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "On Prepare Options Menu");
        return super.onCreateOptionsMenu(menu);
    }

    private String getWebSiteFromEditText() {
        //@TODO basic website address checking before run the search method; like dots or adding https or www (check if needed)
        Log.d(LOG_TAG, etgivenWebSite.getText().toString());
        return etgivenWebSite.getText().toString();
    }

    private void startSensors() {

        //Log.d(LOG_TAG, String.valueOf(Aware_Provider.Aware_Device.CONTENT_URI));
        //Log.d(LOG_TAG, String.valueOf(Aware_Provider.DATABASE_NAME));

        //Activate Accelerometer
        //Aware.setSetting(this, Aware_Preferences.STATUS_ACCELEROMETER, true);
        //Set sampling frequency
        //Aware.setSetting(this, Aware_Preferences.FREQUENCY_ACCELEROMETER, 200000);
        //Apply settings
        //sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));
        //sendBroadcast(new Intent(Aware.ACTION_AWARE_CURRENT_CONTEXT));
        // sendBroadcast(new Intent(Aware.ACTION_AWARE_DEVICE_INFORMATION));

        //Log.d(LOG_TAG, String.valueOf(Accelerometer_Provider.Accelerometer_Data.CONTENT_URI));
    }

    public void searchForWebPage(String webSite) {
        UrlValidator urlToValidate = new UrlValidator(webSite);
            if(urlToValidate.checkUrl()) {
                //@TODO check if it thoruwa onReceiveError??>
                webViewOnPageFinishOnPageStartMethod(urlToValidate.getWebSite());
                try {
                    URL pageToLoadURL = new URL(urlToValidate.getWebSite());
                    new PageStreamReader().execute(pageToLoadURL);
                } catch (MalformedURLException e) {
                        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "Search in google: " + googlePageSearch+webSite);
                        searchForWebPage(googlePageSearch+webSite);
                }
            }else{
                if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "Search in google: " + googlePageSearch+webSite);
                searchForWebPage(googlePageSearch+webSite);
            }

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



    private class PageStreamReader extends AsyncTask<URL, Long, StringBuilder> {

        @Override
        protected StringBuilder doInBackground(URL... pageUrl) {
            try {
                return loadingPageMeasureURLConnection(pageUrl[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(StringBuilder results) {
           // Log.d(LOG_TAG, String.valueOf(results));
        }


        //Using GET command - it will be most probably http.request - http.response time -- to check
        private StringBuilder loadingPageMeasureURLConnection(URL pageToLoadURL) throws IOException {
            startTimeHttpURL = System.nanoTime();

            HttpURLConnection urlConnection = null;
            try {
                urlConnection = (HttpURLConnection) pageToLoadURL.openConnection();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder pageContent = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    pageContent.append(line);
                }
                endTimeHttpURL = System.nanoTime();
                return pageContent;
            }finally
            {
                if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "StreamReader solution: " + Long.toString((endTimeHttpURL - startTimeHttpURL) / 1000000) + " ms");

                if (urlConnection != null)
                    urlConnection.disconnect();
            }
        }
    }


    private void webViewOnPageFinishOnPageStartMethod(String webSite) {
        webPageView.loadUrl(webSite);
        webPageView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                startTime = (new Date()).getTime();
                startTimeSystem = System.currentTimeMillis();

                InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.hideSoftInputFromWindow(browserLayout.getWindowToken(), 0);
                etgivenWebSite.setText(R.string.info_loading);

            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                endTime = (new Date()).getTime();
                endTimeSystem = System.currentTimeMillis();
                LoadTime = endTime - startTime;
                LoadTimeSystem = endTimeSystem - startTimeSystem;
                Log.d(LOG_TAG, webPageView.getUrl() + ": 1)Date " + Long.toString(LoadTime) + "ms 2)System "
                        + LoadTimeSystem + "ms");
                etgivenWebSite.setText("");
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "Error loading page: " + failingUrl);
                searchForWebPage(googlePageSearch + failingUrl);
            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        setIsBrowserActivityVisible(true);
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "BrowserActivty On Resume called");
    }



    @Override
    protected void onStop() {
        super.onStop();
        setIsBrowserActivityVisible(false);
        //stopSensors()
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "BrowserActivty On Stop called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setIsBrowserActivityVisible(false);
        sendBroadcast(new Intent(Aware.ACTION_AWARE_CLEAR_DATA));
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "BrowserActivty On Destroy called");
    }
}
