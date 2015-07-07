package com.monitoringtool.awarebrowser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Browser;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.providers.Aware_Provider;
import com.aware.providers.Network_Provider;
import com.aware.providers.Processor_Provider;
import com.aware.providers.Telephony_Provider;

import java.util.UUID;

import static android.view.KeyEvent.keyCodeToString;
import static com.aware.Aware_Preferences.*;

/**
 * Created by Maria on 2015-06-21.
 */

    /*@@TODO
      @TODO 2) after rotating screen make all saved // crashed otherwise workaround disable rotation for now
     */

public class ToolbarActivity extends ActionBarActivity {
    public static final String LOG_TAG = "WebViewLoadingTime";
    public static final String EXTRA_WEB_SITE = "com.monitoringtool.awarebrowser.WEB_SITE";
    public static final String ACTION_CLOSE_BROWSER = "com.monitoringtool.awarebrowser.ACTION_CLOSE_BROWSER";
    public static final String ACTION_AWARE_READY = "com.monitoringtool.awarebrowser.ACTION_AWARE_READY";

    public static final String RESEARCH_WEBSITE = "http://www.mariak.webd.pl/study/";
    public static final boolean MONITORING_DEBUG_FLAG = false;
    public static final String SHARED_PREF_FILE = "mySharedPref";
    public static final String KEY_IS_BROWSER_SERVICE_RUNNING = "KEY_ID_BROWSER_SERVICE_RUNNING";

    public static final String KEY_FIRST_INSTALL = "KEY_FIRST_INSTALL";


    public static SharedPreferences mySharedPref;
    public static SharedPreferences.Editor editor;


    private boolean isBrowserActivityVisible = false;

    public boolean isBrowserActivityVisible() {
        return isBrowserActivityVisible;
    }




    public void setIsBrowserActivityVisible(boolean isBrowserActivityVisible) {
        this.isBrowserActivityVisible = isBrowserActivityVisible;
    }


    private EditText etgivenWebSite;
    private ImageButton ibBack;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser_toolbar);
        Log.d(LOG_TAG, "on create toolbar");




        mySharedPref = getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE);
        editor = mySharedPref.edit();



        if(mySharedPref.getBoolean(KEY_FIRST_INSTALL, true)){
            if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "First install do not run ESM yet");


        }
        if (!mySharedPref.getBoolean(KEY_IS_BROWSER_SERVICE_RUNNING, false)) {
            if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "RUN Browser_Service");
            Intent browserService = new Intent(getApplicationContext(), Browser_Service.class);
            getApplicationContext().startService(browserService);
        }

    }



    public void prepareToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.browser_toolbar);
        ibBack = (ImageButton) findViewById(R.id.back);
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
                        return runSearch(RESEARCH_WEBSITE);
                    case R.id.action_search:
                        if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "-----action_search-----");
                        return runSearch(getWebSiteFromEditText());
                    default:
                        return false;
                }
            }
        });
    }

    public boolean runSearch(String webSite) {
        Intent browser = new Intent(getApplicationContext(), BrowserActivity.class);
        browser.putExtra(EXTRA_WEB_SITE, webSite);
        startActivity(browser);
        return true;
    }

    private String getWebSiteFromEditText() {
        return etgivenWebSite.getText().toString();
    }
    @Override
    protected void onStart() {
        super.onStart();
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "Toolbar on Start");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "Toolbar on resume");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.menu_browser, menu);
        etgivenWebSite = (EditText) findViewById(R.id.website_name);

        //Add textWacher to EditText
        etgivenWebSite.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_ENTER:
                            return runSearch(getWebSiteFromEditText());
                        default:
                            break;
                    }

                }
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }



    @Override
    protected void onStop() {
        super.onStop();
        if(isBrowserActivityVisible)
            setIsBrowserActivityVisible(false);
        if(!isBrowserActivityVisible) {
            if(!mySharedPref.getBoolean(KEY_FIRST_INSTALL, true)) {
                if(MONITORING_DEBUG_FLAG)Log.d(LOG_TAG, "Send ACTION_CLOSE_BROWSER");
                Intent browserClosed = new Intent();
                browserClosed.setAction(ToolbarActivity.ACTION_CLOSE_BROWSER);
                sendBroadcast(browserClosed);
                //@TODO figure out if I should finish it or not

            }else{
                if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "It is first install no ESM will be set");
                if(mySharedPref.getBoolean(KEY_FIRST_INSTALL, true)) {
                    editor.putBoolean(KEY_FIRST_INSTALL, false);
                    editor.commit();
                    stopService(new Intent(getApplicationContext(), Browser_Service.class));
                }
            }
            finish();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "Toolbar terminated");

    }
}
