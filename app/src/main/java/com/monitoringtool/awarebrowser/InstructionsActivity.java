package com.monitoringtool.awarebrowser;

import android.os.Bundle;
import android.view.Menu;
import android.webkit.WebSettings;
import android.webkit.WebView;
/**
 * Created by Maria on 2015-06-21.
 */
public class InstructionsActivity extends BrowserActivity{


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setIsInstructionsActivityVisible(true);
        setContentView(R.layout.activity_instructions);
        super.prepareToolbar();

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        super.setIsInstructionsActivityVisible(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        super.setIsInstructionsActivityVisible(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        super.setIsInstructionsActivityVisible(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        super.setIsInstructionsActivityVisible(false);
    }
}
