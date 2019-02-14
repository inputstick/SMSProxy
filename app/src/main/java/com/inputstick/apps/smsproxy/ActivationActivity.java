package com.inputstick.apps.smsproxy;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ActivationActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private String key;

    private CardView cardViewActivation;
    private TextView textViewActivationSMSPermissionStatus;
    private Button buttonActivationAllow;
    private Button buttonActivationDeny;

    private CardView cardViewDeactivation;
    private Button buttonDeactivationDismiss;

    private CardView cardViewError;
    private Button buttonErrorDismiss;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activation);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Activation:
        cardViewActivation = findViewById(R.id.cardViewActivation);
        textViewActivationSMSPermissionStatus = findViewById(R.id.textViewActivationSMSPermissionStatus);
        buttonActivationAllow = findViewById(R.id.buttonActivationAllow);
        buttonActivationAllow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setKey(key);
                setResult(RESULT_OK);
                finish();
            }
        });
        buttonActivationDeny = findViewById(R.id.buttonActivationDeny);
        buttonActivationDeny.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setKey(null);
                finish();
            }
        });
        //Deactivation
        cardViewDeactivation = findViewById(R.id.cardViewDeactivation);
        buttonDeactivationDismiss = findViewById(R.id.buttonDeactivationDismiss);
        buttonDeactivationDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        //Error
        cardViewError = findViewById(R.id.cardViewError);
        buttonErrorDismiss = findViewById(R.id.buttonErrorDismiss);
        buttonErrorDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        String action = getIntent().getAction();
        if (Const.SMS_PROXY_ACTION_ACTIVATE.equals(action)) {
            key = getIntent().getStringExtra(Const.SMS_PROXY_EXTRA_KP2A_KEY);
            if (key == null) {
                cardViewActivation.setVisibility(View.GONE);
                cardViewDeactivation.setVisibility(View.GONE);
            } else {
                cardViewDeactivation.setVisibility(View.GONE);
                cardViewError.setVisibility(View.GONE);
                updateSMSPermissionInfo(true);
                if (ContextCompat.checkSelfPermission(ActivationActivity.this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                    Utils.showRequestPermissionsInfoAlertDialog(this);
                    updateSMSPermissionInfo(false);
                } else {
                    updateSMSPermissionInfo(true);
                }
            }

        } else if (Const.SMS_PROXY_ACTION_DEACTIVATE.equals(action)) {
            setKey(null);
            if (SMSService.isRunning()) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(Const.SMS_PROXY_PACKAGE, Const.SMS_PROXY_SERVICE));
                stopService(intent);
            }

            cardViewActivation.setVisibility(View.GONE);
            cardViewError.setVisibility(View.GONE);
        } else {
            cardViewActivation.setVisibility(View.GONE);
            cardViewDeactivation.setVisibility(View.GONE);
        }

        setResult(RESULT_CANCELED);
    }

    private void updateSMSPermissionInfo(boolean hasPermission) {
        if (hasPermission) {
            textViewActivationSMSPermissionStatus.setVisibility(View.GONE);
        } else {
            textViewActivationSMSPermissionStatus.setVisibility(View.VISIBLE);
        }
        buttonActivationAllow.setEnabled(hasPermission);
    }

    private void setKey(String key) {
        SharedPreferences.Editor editor = prefs.edit();
        if (key == null) {
            editor.remove(Const.PREF_KP2APLUGIN_KEY);
        } else {
            editor.putString(Const.PREF_KP2APLUGIN_KEY, key);
        }
        editor.apply();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == Const.REQUEST_CODE_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateSMSPermissionInfo(true);
                if (SMSService.isRunning()) {
                    SMSService.registerSMSReceiver();
                }
            }
        }
    }
}
