package com.monitoringtool.awarebrowser;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;

/**
 * Created by Maria on 2015-06-21.
 */
public class InstructionsActivity extends BrowserActivity{

    private EditText etgivenWebSite;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "InstructionActivity Create");
        super.setIsInstructionsActivityVisible(true);
        setContentView(R.layout.activity_instructions);
        etgivenWebSite = (EditText) findViewById(R.id.website_name);

        super.prepareToolbar();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "InstructionActivity Resume");
        super.setIsInstructionsActivityVisible(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "InstructionActivity Start");
        super.setIsInstructionsActivityVisible(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "InstructionActivity Stop");
        super.setIsInstructionsActivityVisible(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "InstructionActivity Destroy");
        super.setIsInstructionsActivityVisible(false);
    }
}
