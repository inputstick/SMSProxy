package com.inputstick.apps.smsproxy;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;

public class Utils {

    public static void showRequestPermissionsInfoAlertDialog(final Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.title_sms_permission_request);
        builder.setMessage(R.string.text_sms_permission_request);
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(activity,  new String[]{Manifest.permission.READ_SMS}, Const.REQUEST_CODE_SMS);
                    }
                });
        builder.setCancelable(false);
        builder.show();
    }

}
