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

public class BrowserClosedReceiver extends BroadcastReceiver{

    private static final boolean MONITORING_DEBUG_FLAG = BrowserActivity.MONITORING_DEBUG_FLAG;
    private static final String LOG_TAG_ESM_CREATE = "AB:ESM_CREATE";
    private static final String ACTION_AWARE_CLOSE_BROWSER = BrowserActivity.ACTION_AWARE_CLOSE_BROWSER;
    @Override
        public void onReceive(Context context, Intent intent) {
            if(MONITORING_DEBUG_FLAG) Log.d(LOG_TAG_ESM_CREATE, "Receiver stop Browser Broadcast.");

            final String esmRating =
                    "[{'esm': {" +
                            "'esm_type': " + ESM.TYPE_ESM_LIKERT +"," +
                            "'esm_title': '" + context.getResources().getString(R.string.title_rating) + "'," +
                            "'esm_instructions': '" + context.getResources().getString(R.string.esm_rating_instructions) + "'," +
                            "'esm_likert_max': 5," +
                            "'esm_likert_max_label': '"+ context.getResources().getString(R.string.esm_rating_max) +"'," +
                            "'esm_likert_min_label': '"+ context.getResources().getString(R.string.esm_rating_min) +"'," +
                            "'esm_likert_step': 1," +
                            "'esm_submit': '" + context.getResources().getString(R.string.esm_button_next) + "'," +
                            "'esm_expiration_threashold': 60," +
                            "'esm_trigger': 'AwareBro answer'" +
                            "}},";


            final String esmContext =
                    "{'esm': {" +
                            "'esm_type': " + ESM.TYPE_ESM_CHECKBOX +"," +
                            "'esm_title': '" + context.getResources().getString(R.string.title_context) + "'," +
                            "'esm_instructions': '" + context.getResources().getString(R.string.esm_context_instructions) + "'," +
                            "'esm_checkboxes':['"+ context.getResources().getString(R.string.esm_context_answer1) +"','"+context.getResources().getString(R.string.esm_context_answer2)+"','"+context.getResources().getString(R.string.esm_context_answer3)+"','"+context.getResources().getString(R.string.esm_context_answer4)+"', '"+context.getResources().getString(R.string.esm_context_answer5) +"']," +
                            "'esm_submit': '" + context.getResources().getString(R.string.esm_button_next) + " '," +
                            "'esm_expiration_threashold': 60," +
                            "'esm_trigger': 'AwareBro answer'" +
                            "}},";


            //can be probably replaced by Google Recognition System -- or use for comapring the reuslts
            final String esmMovement =
                    "{'esm': {" +
                    "'esm_type': " + ESM.TYPE_ESM_RADIO +"," +
                    "'esm_title': '" + context.getResources().getString(R.string.title_movement) + "'," +
                    "'esm_instructions': '" + context.getResources().getString(R.string.esm_movement_instructions) + "'," +
                    "'esm_radios':['"+ context.getResources().getString(R.string.esm_movement_answer1) +"','"+context.getResources().getString(R.string.esm_movement_answer2)+"','"+context.getResources().getString(R.string.esm_movement_answer3)+"'],"+
                    "'esm_submit': '" + context.getResources().getString(R.string.esm_button_next) + " '," +
                    "'esm_expiration_threashold': 60," +
                    "'esm_trigger': 'AwareBro answer'" +
                    "}},";

            final String esmDelaysAcceptance =
                    "{'esm': {" +
                    "'esm_type': " + ESM.TYPE_ESM_QUICK_ANSWERS +"," +
                    "'esm_title': '" + context.getResources().getString(R.string.title_delays_accept) + "'," +
                    "'esm_instructions': '" + context.getResources().getString(R.string.esm_delays_accept_instructions) + "'," +
                    "'esm_quick_answers':['" + context.getResources().getString(R.string.esm_answer_no) + "','" + context.getResources().getString(R.string.esm_answer_yes) + "']," +
                    "'esm_expiration_threashold': 60," +
                    "'esm_trigger': 'AwareBro answer'" +
                    "}}]";

            final String esmQuestionnaire = esmRating + esmContext + esmMovement + esmDelaysAcceptance;

            String action = intent.getAction();
            if(action.equals(ACTION_AWARE_CLOSE_BROWSER)) {
                Intent esm = new Intent(ESM.ACTION_AWARE_QUEUE_ESM);
                esm.putExtra(ESM.EXTRA_ESM, esmQuestionnaire);
                context.sendBroadcast(esm);
                Toast.makeText(context, context.getResources().getString(R.string.info_loading_esm), Toast.LENGTH_SHORT).show();
            }
        }
    }
