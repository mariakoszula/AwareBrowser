package com.monitoringtool.awarebrowser;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatCallback;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
/**
 * Created by Maria on 2015-06-21.
 */
public class AboutActivity extends MainActivity{


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        super.prepareToolbar();
        WebView about_us = (WebView) findViewById(R.id.about_us);
        WebSettings settings = about_us.getSettings();
        settings.setJavaScriptEnabled(true);
        about_us.loadUrl("http://www.mariak.webd.pl/study/");
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }
}
