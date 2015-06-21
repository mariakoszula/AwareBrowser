package com.monitoringtool.awarebrowser;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatCallback;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
/**
 * Created by Maria on 2015-06-21.
 */
public class AboutActivity extends Activity implements AppCompatCallback {


    private AppCompatDelegate delegate;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        delegate = AppCompatDelegate.create(this,this);
        delegate.onCreate(savedInstanceState);

        delegate.setContentView(R.layout.activity_about);

        Toolbar toolbar = (Toolbar) findViewById(R.id.browser_toolbar);
        delegate.setSupportActionBar(toolbar);

        if(toolbar!=null){
            toolbar.inflateMenu(R.menu.menu_browser);
            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    Log.d("dupa", item.getTitle().toString());
                    return false;
                }
            });
        }

        WebView about_us = (WebView) findViewById(R.id.about_us);
        WebSettings settings = about_us.getSettings();
        settings.setJavaScriptEnabled(true);
        about_us.loadUrl("http://www.mariak.webd.pl/study/");
    }

    @Override
    public void onSupportActionModeStarted(ActionMode mode) {

    }

    @Override
    public void onSupportActionModeFinished(ActionMode mode) {

    }

    @Nullable
    @Override
    public ActionMode onWindowStartingSupportActionMode(ActionMode.Callback callback) {
        return null;
    }
}
