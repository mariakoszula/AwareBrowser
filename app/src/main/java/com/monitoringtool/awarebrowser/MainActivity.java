package com.monitoringtool.awarebrowser;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

/**
 * Created by Maria on 2015-06-21.
 */
public class MainActivity extends ActionBarActivity {
    public static final String LOG_TAG = "WebViewLoadingTime";
    private EditText etgivenWebSite;
    public void setIsBrowserActivityVisible(boolean isBrowserActivityVisible) {
        this.isBrowserActivityVisible = isBrowserActivityVisible;
    }

    private boolean isBrowserActivityVisible=false;

    public void setIsAboutActctivityVisable(boolean isAboutActctivityVisable) {
        this.isAboutActctivityVisable = isAboutActctivityVisable;
    }

    private boolean isAboutActctivityVisable=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }
    public void prepareToolbar(){
        final Toolbar toolbar = (Toolbar) findViewById(R.id.browser_toolbar);
        setSupportActionBar(toolbar);


        toolbar.inflateMenu(R.menu.menu_browser);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                Log.d(LOG_TAG, String.valueOf(item.getItemId()));
                Log.d(LOG_TAG, "action_about: " + R.string.action_about);
                switch (item.getItemId()) {
                    case R.id.action_about:
                        if (!isAboutActctivityVisable) {
                            Intent about = new Intent(MainActivity.this, AboutActivity.class);
                            startActivity(about);
                            return true;
                        }
                    case R.id.action_search:
                        String givenWebSite = getWebSiteFromEditText();
                        if (!isBrowserActivityVisible) {
                            Intent browser = new Intent(MainActivity.this, BrowserActivity.class);
                            browser.putExtra("EXTRA_WEB_SITE", givenWebSite);
                            startActivity(browser);

                        } else {
                            Log.d(LOG_TAG, "refresh view" + givenWebSite);
                        }
                        return true;


                    default:
                        return false;
                }
            }
        });
    }

    private String getWebSiteFromEditText() {
        //@TODO basic website address checking before run the search method; like dots or adding https or www (check if needed)
        etgivenWebSite = (EditText) findViewById(R.id.website_name);
        return etgivenWebSite.getText().toString();
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_browser, menu);

        return super.onCreateOptionsMenu(menu);
    }

}
