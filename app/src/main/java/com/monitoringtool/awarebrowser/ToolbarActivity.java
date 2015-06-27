package com.monitoringtool.awarebrowser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
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
import android.widget.LinearLayout;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

/**
 * Created by Maria on 2015-06-21.
 */
public class ToolbarActivity extends ActionBarActivity {
    public static final String LOG_TAG = "WebViewLoadingTime";
    public static final String EXTRA_WEB_SITE = "com.monitoringtool.awarebrowser.WEB_SITE";
    public static final String RESEARCH_WEBSITE = "http://www.mariak.webd.pl/study/";
    public static final boolean MONITORING_DEBUG_FLAG = true;


    private boolean isBrowserActivityVisible = false;
    private boolean isInstructionsActivityVisible = false;


    private EditText etgivenWebSite;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser_toolbar);
        prepareToolbar();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.menu_browser, menu);
        etgivenWebSite = (EditText) findViewById(R.id.website_name);
        return super.onCreateOptionsMenu(menu);
    }

    public void setIsBrowserActivityVisible(boolean isBrowserActivityVisible) {
        this.isBrowserActivityVisible = isBrowserActivityVisible;
    }

    public void setIsInstructionsActivityVisible(boolean isInstructionsActivityVisible) {
        this.isInstructionsActivityVisible = isInstructionsActivityVisible;
    }

    public void prepareToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.browser_toolbar);
        setSupportActionBar(toolbar);


        toolbar.inflateMenu(R.menu.menu_browser);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                String givenWebSite = null;
                switch (item.getItemId()) {
                    case R.id.action_about:
                        if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "action_about");
                        givenWebSite = RESEARCH_WEBSITE;
                        return runSearch(givenWebSite);
                    case R.id.action_search:
                        if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "action_search");
                        givenWebSite = getWebSiteFromEditText();
                        return runSearch(givenWebSite);
                    case R.id.action_instruction:
                        if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "action_instruction");
                        if (!isInstructionsActivityVisible) {
                            Intent instructions = new Intent(getBaseContext(), InstructionsActivity.class);
                            startActivity(instructions);
                            return true;
                        }
                    default:
                        return false;
                }
            }
        });
    }

    private boolean runSearch(String webSite) {
        Intent browser = new Intent(getBaseContext(), BrowserActivity.class);
/*            browser.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);*/
        browser.putExtra(EXTRA_WEB_SITE, webSite);
        startActivity(browser);
        return true;
    }

    private String getWebSiteFromEditText() {
        Log.d(LOG_TAG, etgivenWebSite.getText().toString());
        return etgivenWebSite.getText().toString();
    }


}
