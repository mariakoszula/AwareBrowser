package com.monitoringtool.awarebrowser;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import android.widget.Toolbar;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.providers.ESM_Provider;
import com.google.android.gms.maps.model.TileOverlay;

/**
 * Created by Maria on 2015-06-29.
 */

public class BrowserClosed_Receiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if(ToolbarActivity.MONITORING_DEBUG_FLAG) Log.d(ToolbarActivity.LOG_TAG, "Receiver stop Browser Broadcast.");

            final String esmRating =
                    "[{'esm': {" +
                            "'esm_type': " + ESM.TYPE_ESM_LIKERT +"," +
                            "'esm_title': '" + context.getResources().getString(R.string.title_rating) + "'," +
                            "'esm_instructions': '" + context.getResources().getString(R.string.esm_rating_instructions) + "'," +
                            "'esm_likert_max': 5," +
                            "'esm_likert_max_label': '"+ context.getResources().getString(R.string.esm_rating_max) +"'," +
                            "'esm_likert_min_label': '"+ context.getResources().getString(R.string.esm_rating_min) +"'," +
                            "'esm_likert_step': 1," +
                            "'esm_submit': 'OK'," +
                            "'esm_expiration_threashold': 60," +
                            "'esm_trigger': 'AwareBro answer'" + //maybe here some session ID which will be in browser provider and borwsers as well in shared prefrences
                            "}}]";
            final String esmContext =    "";
            final String esmPlace =    "";
            final String esmQuestionnaire = "";

             /*@TODO preapre acctual questionaires */
            String action = intent.getAction();
            if(action.equals(ToolbarActivity.ACTION_CLOSE_BROWSER)) {
                Intent esm = new Intent(ESM.ACTION_AWARE_QUEUE_ESM);
                esm.putExtra(ESM.EXTRA_ESM, esmRating);
                context.sendBroadcast(esm);
                Toast.makeText(context, context.getResources().getString(R.string.info_loading), Toast.LENGTH_LONG).show();
            }
        }
    }
