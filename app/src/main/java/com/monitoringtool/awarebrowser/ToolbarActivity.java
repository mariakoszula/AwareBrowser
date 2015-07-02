package com.monitoringtool.awarebrowser;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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

import static com.aware.Aware_Preferences.*;

/**
 * Created by Maria on 2015-06-21.
 */
public class ToolbarActivity extends ActionBarActivity {
    public static final String LOG_TAG = "WebViewLoadingTime";
    public static final String PACKAGE_NAME = "com.monitoringtool.awarebrowser";
    public static final String EXTRA_WEB_SITE = "com.monitoringtool.awarebrowser.WEB_SITE";
    public static final String ACTION_CLOSE_BROWSER = "com.monitoringtool.awarebrowser.ACTION_CLOSE_BROWSER";
    public static final String DASHBOARD_STUDY_URL = "https://api.awareframework.com/index.php/webservice/index/403/yqA2zgDrJOPl";


    public static final String RESEARCH_WEBSITE = "http://www.mariak.webd.pl/study/";
    public static final boolean MONITORING_DEBUG_FLAG = true;
    public static final String SHARED_PREF_FILE = "mySharedPref";
    public static final String KEY_IS_SENSOR_RUNNING = "KEY_ID_SENSOR_RUNNING";
    public static final String KEY_UNIQUE_DEVICE_ID = "KEY_UNIQUE_DEVICE_ID";
    public static final String KEY_FIRST_INSTALL = "KEY_ID_STOP_SENSORS";


  //  private boolean isSensorRunning = false; //sharedPreferences or sth like that so it wanto be stoped and start every time webSite is searched and maybe the same value for applicatioIsRunning
    public static SharedPreferences mySharedPref;
    public static SharedPreferences.Editor editor;
    private boolean isInstructionsActivityVisible = false;
    private boolean isBrowserActivityVisible = false;

    /*@TODO 1) prepare Content provider for Browser and check if it creates proper table
      @TODO 2) check if data are saved properly with Device_ID - unique for each session plus Device_unique_ID - device id the same between sessions in Browser_Provider
      @TODO 3) try to synchronize this with server Aware Dashboard
      @TODO 4) after rotating screen make all saved // crashed otherwise
     */

    public void setIsBrowserActivityVisible(boolean isBrowserActivityVisible) {
        this.isBrowserActivityVisible = isBrowserActivityVisible;
    }


    //@TODO delay starting application after service ends
    private EditText etgivenWebSite;
    private ImageButton ibBack;

    public void setIsInstructionsActivityVisible(boolean isInstructionsActivityVisible) {
        this.isInstructionsActivityVisible = isInstructionsActivityVisible;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser_toolbar);
        Log.d(LOG_TAG, "on create toolbar");

       // sendBroadcast(new Intent(Aware.ACTION_AWARE_DEVICE_INFORMATION));

        mySharedPref = getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE);
        editor = mySharedPref.edit();

        if( mySharedPref.getString(KEY_UNIQUE_DEVICE_ID, "").length() == 0 ) {
            UUID uuid = UUID.randomUUID();
            editor.putString(KEY_UNIQUE_DEVICE_ID, uuid.toString());
            editor.commit();
        }

        UUID uuid_session_ID = UUID.randomUUID();
        Aware.setSetting(this, Aware_Preferences.DEVICE_ID, uuid_session_ID.toString());

        prepareToolbar();


        //@TODO idea: use aware_device id unique for each session, will be easier to recognize it later
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "Aware device id: " + Aware.getSetting(this, DEVICE_ID));


    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "Start Toolbar KEY_IS_SENSOR_RUNNING " + String.valueOf(mySharedPref.getBoolean(KEY_IS_SENSOR_RUNNING, false)));
        if(!mySharedPref.getBoolean(SHARED_PREF_FILE, false)){
            if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "Start sensors");
            startSensors();
            editor.putBoolean(KEY_IS_SENSOR_RUNNING, true);
            editor.commit();
        }



    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "TOolbar on resume");




    }

    private void startSensors() {
        if(MONITORING_DEBUG_FLAG) Aware.setSetting(this, DEBUG_FLAG, true);
        //Activate Aware Sensors
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "Aware Content_URI Aware Device" + String.valueOf(Aware_Provider.Aware_Device.CONTENT_URI));
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "Aware Database Name" + String.valueOf(Aware_Provider.DATABASE_NAME));


        /*@TODO send message to the remote server
        * @TODO Preapre Browser_Provider
        * */

        //@TODO prepare aware in separate thread
        //Network Sensor
        Aware.setSetting(this, STATUS_NETWORK_EVENTS, true);
        Aware.setSetting(this, STATUS_NETWORK_TRAFFIC, true);
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, String.valueOf(Network_Provider.Network_Data.CONTENT_URI));

        //Telephony Sensor
        //Telephony Sensor (only when in Network sensor -- only if broadcast Mobile_ON and stop when Mobile_off)? will now from here the type of telephony
        Aware.setSetting(this, STATUS_TELEPHONY, true);
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, String.valueOf(Telephony_Provider.Telephony_Data.CONTENT_URI));


        //ESM Sensor
        //Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, true);


       // getApplicationContext().startService(new Intent(this, BrowserClosed_Receiver.class));

        //Procesor load problably only for tests if it is not taking too much processor
        if(MONITORING_DEBUG_FLAG){
            Aware.setSetting(this, STATUS_PROCESSOR, false);
            Log.d(LOG_TAG, String.valueOf(Processor_Provider.Processor_Data.CONTENT_URI));

        }

        startService(new Intent(getApplicationContext(), Browser_Service.class));

        //WebService
        Aware.setSetting(this, STATUS_WEBSERVICE, true);
        Aware.setSetting(this, WEBSERVICE_SERVER, DASHBOARD_STUDY_URL);
       // Aware.setSetting(this, FREQUENCY_WEBSERVICE, 10);
        //or in specified time Aware.Action_Aware_sync_data
       //@TODO Google Activity Recognition plugin


        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));


    }

    private void stopSensors() {
       // Toast.makeText(getApplicationContext(), "Stop sensor after closing app!!", Toast.LENGTH_LONG).show();
        //Network Sensor
        Aware.setSetting(this, STATUS_NETWORK_EVENTS, false);
        Aware.setSetting(this, STATUS_NETWORK_TRAFFIC, false);

        //Telephony Sensor
        //Telephony Sensor (only when in Network sensor -- only if broadcast Mobile_ON and stop when Mobile_off)? will now from here the type of telephony
        Aware.setSetting(this, STATUS_TELEPHONY, false);

        //Procesor load problably only for tests if it is not taking too much processor
        if(MONITORING_DEBUG_FLAG){
            Aware.setSetting(this, STATUS_PROCESSOR, false);
        }
        stopService(new Intent(getApplicationContext(), Browser_Service.class));

        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.menu_browser, menu);
        etgivenWebSite = (EditText) findViewById(R.id.website_name);
        return super.onCreateOptionsMenu(menu);
    }




    public void prepareToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.browser_toolbar);
        ibBack = (ImageButton) findViewById(R.id.back);
        ibBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Back", Toast.LENGTH_LONG).show();
            }
        });



        setSupportActionBar(toolbar);


        toolbar.inflateMenu(R.menu.menu_browser);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                String givenWebSite = null;
                switch (item.getItemId()) {
                    case R.id.action_about:
                        if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "-----action_about-----");
                        givenWebSite = RESEARCH_WEBSITE;
                        return runSearch(givenWebSite);
                    case R.id.action_search:
                        if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "-----action_search-----");
                        givenWebSite = getWebSiteFromEditText();
                        return runSearch(givenWebSite);
                    case R.id.action_instruction:
                        if (MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "-----action_instruction-----");
                        if (!isInstructionsActivityVisible) {
                            setIsInstructionsActivityVisible(true);
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
        Intent browser = new Intent(getApplicationContext(), BrowserActivity.class);
        browser.putExtra(EXTRA_WEB_SITE, webSite);
        startActivity(browser);
        return true;
    }

    private String getWebSiteFromEditText() {
        return etgivenWebSite.getText().toString();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(LOG_TAG, "toolbar on stop instrucions " + String.valueOf(isInstructionsActivityVisible) + "browser " + String.valueOf(isBrowserActivityVisible));
        if(isBrowserActivityVisible)
            setIsBrowserActivityVisible(false);
        if(!isInstructionsActivityVisible && !isBrowserActivityVisible) {
          //  startESMActivity();

            if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "Starting service");
            Intent esmService = new Intent(getApplicationContext(), ESM_Service.class);
           // esmService.putExtra("TIME_OF_STOP_BROWSER", )
            getApplicationContext().startService(esmService);


            Log.d(LOG_TAG, "ESM started. Stop Toolbar KEY_IS_SENSOR_RUNNING " + String.valueOf(mySharedPref.getBoolean(KEY_IS_SENSOR_RUNNING, false)));
            if(mySharedPref.getBoolean(KEY_IS_SENSOR_RUNNING, false))
                stopSensors();
                if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "Stop sensors");
        }
        finish();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG, "Toolbar terminated");

    }





}
