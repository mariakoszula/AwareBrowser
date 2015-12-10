package com.monitoringtool.awarebrowser;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.Activity;

public class EndMessageActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end_message);
        AlertDialog endMassage = new AlertDialog.Builder(this).create();
        endMassage.setTitle(this.getResources().getString(R.string.app_name));
        endMassage.setMessage(this.getResources().getString(R.string.end_of_study_notification));
        endMassage.setButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        endMassage.setIcon(R.drawable.ic_browser);
        endMassage.show();

    }

}
