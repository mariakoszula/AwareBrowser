package com.monitoringtool.awarebrowser;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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
import java.util.Date;


public class Browser extends ActionBarActivity {
    public static final String AUTHORITY = "com.monitoringtool.awarebrowser";
    private WebView webPage;
    private String testURL = "http://www.onet.pl";
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
    private static final String LOG_TAG = "WebViewLoadingTime";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        try {
            URL pageToLoadURL = new URL(testURL);
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


        //Activate Accelerometer
        Aware.setSetting(this, Aware_Preferences.STATUS_ACCELEROMETER, true);
        //Set sampling frequency
        Aware.setSetting(this, Aware_Preferences.FREQUENCY_ACCELEROMETER, 200000);
        //Apply settings
        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_browser, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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


        webPage.loadUrl(testURL);
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
}
