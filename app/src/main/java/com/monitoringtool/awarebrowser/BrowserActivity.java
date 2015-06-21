package com.monitoringtool.awarebrowser;

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
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.providers.Accelerometer_Provider;
import com.aware.providers.Aware_Provider;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;


public class BrowserActivity extends MainActivity {

    private WebView webPage;
    private boolean isActivityVisable;
    private String defaultSite = "http://www.google.pl";
    private long LoadTime = 0;
    private long LoadTimeHttpURL = 0;
    private long LoadTimeSystem = 0;
    private long LoadTimeNano = 0;
    private long startTime = 0;
    private long startTimeSystem = 0;
    private long startTimeNano = 0;
    private long startTimeHttpURL = 0;
    private long endTime = 0;
    private long endTimeHttpURL = 0;
    private long endTimeSystem = 0;
    private long endTimeNano = 0;

/*@TODO set setters if applicationvisilbe or not for bothBrowsser and About*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);
        super.prepareToolbar();
        Log.d(LOG_TAG, String.valueOf(Build.VERSION.SDK_INT));



        try {
            URL pageToLoadURL = new URL(defaultSite);
            new PageStreamReader().execute(pageToLoadURL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        //test second method after 20s
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                webViewOnPageFinishOnPageStartMethod();
            }
        }, 20000);


        Log.d(LOG_TAG, String.valueOf(Aware_Provider.Aware_Device.CONTENT_URI));
        Log.d(LOG_TAG, String.valueOf(Aware_Provider.DATABASE_NAME));

        //Activate Accelerometer
        Aware.setSetting(this, Aware_Preferences.STATUS_ACCELEROMETER, true);
        //Set sampling frequency
        Aware.setSetting(this, Aware_Preferences.FREQUENCY_ACCELEROMETER, 200000);
        //Apply settings
        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));
        sendBroadcast(new Intent(Aware.ACTION_AWARE_CURRENT_CONTEXT));
        sendBroadcast(new Intent(Aware.ACTION_AWARE_DEVICE_INFORMATION));

        Log.d(LOG_TAG, String.valueOf(Accelerometer_Provider.Accelerometer_Data.CONTENT_URI));
    }
    public  boolean getIsActivityVisable(){
        return isActivityVisable;
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
            Log.d(LOG_TAG, String.valueOf(results));
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
            } finally {
                Log.d(LOG_TAG, "StreamReader solution: " + Long.toString((endTimeHttpURL - startTimeHttpURL) / 1000000) + " ms");
                urlConnection.disconnect();
            }
        }
    }


    private void webViewOnPageFinishOnPageStartMethod() {
        webPage = (WebView) findViewById(R.id.webPageView);

        //Enable Javascript, webView does not allow JS by default
        WebSettings settings = webPage.getSettings();
        settings.setJavaScriptEnabled(true);


        webPage.loadUrl(defaultSite);
        webPage.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                startTime = (new Date()).getTime();
                startTimeSystem = System.currentTimeMillis();
                startTimeNano = System.nanoTime();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                endTime = (new Date()).getTime();
                endTimeSystem = System.currentTimeMillis();
                endTimeNano = System.nanoTime();
                LoadTime = endTime - startTime;
                LoadTimeSystem = endTimeSystem - startTimeSystem;
                LoadTimeNano = endTimeNano - startTimeNano;
                Log.d(LOG_TAG, webPage.getUrl() + ": 1)Date " + Long.toString(LoadTime) + "ms 2)System "
                        + LoadTimeSystem + " ms 3)System nano " + LoadTimeNano / 1000000 + " ms");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sendBroadcast(new Intent(Aware.ACTION_AWARE_CLEAR_DATA));
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_browser, menu);

        return super.onCreateOptionsMenu(menu);
    }

}
