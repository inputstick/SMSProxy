package com.inputstick.apps.smsproxy;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static boolean requestDisplayed;

    private SharedPreferences prefs;
    private TextView textViewSMSPermissionStatus;
    private TextView textViewPluginIntegrationStatus;
    private Button buttonWebpage;
    private Button buttonSource;
    private Button buttonSMSPermission;
    private Button buttonKP2APluginSettings;
    private CheckBox checkBoxNotification;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        textViewSMSPermissionStatus = findViewById(R.id.textViewSMSPermissionStatus);
        textViewPluginIntegrationStatus = findViewById(R.id.textViewPluginIntegrationStatus);
        buttonWebpage = findViewById(R.id.buttonWebpage);
        buttonSource = findViewById(R.id.buttonSource);
        buttonSMSPermission = findViewById(R.id.buttonSMSPermission);
        buttonKP2APluginSettings = findViewById(R.id.buttonKP2APluginSettings);
        checkBoxNotification = findViewById(R.id.checkBoxNotification);

        //hide error info card if SMS is supported:
        CardView cardViewNotSupported = findViewById(R.id.cardViewNotSupported);
        PackageManager mgr = getPackageManager();
        if (mgr.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            cardViewNotSupported.setVisibility(View.GONE);
        }


        buttonWebpage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Const.SMS_PROXY_URL_INFO_AND_DOWNLOAD));
                startActivity(browserIntent);
            }
        });

        buttonSource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Const.SMS_PROXY_URL_SOURCE)); //TODO github URL
                startActivity(browserIntent);
            }
        });


        buttonSMSPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.showRequestPermissionsInfoAlertDialog(MainActivity.this);
            }
        });

        buttonKP2APluginSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(Const.PACKAGE_KP2A_PLUGIN);
                if (launchIntent != null) {
                    startActivity(launchIntent);//null pointer check in case package name was not found
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + Const.PACKAGE_KP2A_PLUGIN));
                    intent.setPackage("com.android.vending");
                    startActivity(intent);
                }
            }
        });

        checkBoxNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean newValue = checkBoxNotification.isChecked();
                checkBoxNotification.setChecked(newValue);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(Const.PREF_NOTIFICATION, newValue);
                editor.apply();
                if (SMSService.isRunning()) {
                    if (newValue) {
                        SMSService.showNotification();
                    } else {
                        SMSService.hideNotification();
                    }
                }
            }
        });

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            if ( !requestDisplayed) {
                Utils.showRequestPermissionsInfoAlertDialog(this);
                requestDisplayed = true;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            textViewSMSPermissionStatus.setVisibility(View.VISIBLE);
            buttonSMSPermission.setVisibility(View.VISIBLE);
        } else {
            textViewSMSPermissionStatus.setVisibility(View.GONE);
            buttonSMSPermission.setVisibility(View.GONE);
        }

        if (prefs.contains(Const.PREF_KP2APLUGIN_KEY)) {
            textViewPluginIntegrationStatus.setVisibility(View.GONE);
        } else {
            textViewPluginIntegrationStatus.setVisibility(View.VISIBLE);
        }

        checkBoxNotification.setChecked(prefs.getBoolean(Const.PREF_NOTIFICATION, false));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == Const.REQUEST_CODE_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateUI();
                if (SMSService.isRunning()) {
                    SMSService.registerSMSReceiver();
                }
            }
        }
    }
}
