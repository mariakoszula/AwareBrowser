package com.monitoringtool.awarebrowser;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.aware.Aware;

import static com.aware.Aware_Preferences.STATUS_WEBSERVICE;
import static com.aware.Aware_Preferences.WEBSERVICE_SERVER;

/**
 * Created by Maria on 2015-06-21.
 */
public class InstructionsActivity extends ToolbarActivity{
    public static final String DASHBOARD_STUDY_URL = "https://api.awareframework.com/index.php/webservice/index/403/yqA2zgDrJOPl";
    private Button eraseData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setIsInstructionsActivityVisible(true);
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "InstructionActivity Create");
        setContentView(R.layout.activity_instructions);
        eraseData = (Button) findViewById(R.id.erasedata);
        eraseData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Erase", Toast.LENGTH_LONG).show();
               /* Aware.setSetting(getApplicationContext(), STATUS_WEBSERVICE, true);
                Aware.setSetting(getApplicationContext(), WEBSERVICE_SERVER, DASHBOARD_STUDY_URL);
                sendBroadcast(new Intent(Aware.ACTION_AWARE_CLEAR_DATA));*/
            }
        });
        super.prepareToolbar();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        super.setIsInstructionsActivityVisible(true);
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "InstructionActivity Resume");
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
    }
}
