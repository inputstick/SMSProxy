package com.inputstick.apps.smsproxy;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsMessage;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class SMSService extends Service {

    private static final int TIMER_LIMIT_MS = 		299000; //4min 59s
    private static final int TIMER_INTERVAL_MS = 	 60000; //60s

    private static boolean isRunning;
    private static long stopTime;
    private static boolean smsReceiverRegistered;

    private static SharedPreferences prefs;

    private static Context mCtx;
    private static NotificationManager mNotificationManager;
    private static NotificationCompat.Builder mNotificationBuilder;

    private Handler mHandler = new Handler();
    private Runnable mTimerTask = new Runnable() {
        public void run() {
            final long time = System.currentTimeMillis();
            if (time >= stopTime) {
                stopSelf();
            }
            mHandler.postDelayed(mTimerTask, TIMER_INTERVAL_MS);
        }
    };

    private static final BroadcastReceiver smsReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            sendSMSBroadcast(bundle);
        }
    };

    static void sendSMSBroadcast(Bundle bundle) {
        Object[] pdus = (Object[]) bundle.get("pdus");
        for (int i = 0; i < pdus.length; i++) {
            SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdus[i]);
            String smsText = smsMessage.getMessageBody();
            String smsSender = smsMessage.getDisplayOriginatingAddress();

            String key = prefs.getString(Const.PREF_KP2APLUGIN_KEY, null);
            if (key != null) {
                try {
                    Mac sha256_HMAC;
                    sha256_HMAC = Mac.getInstance("HmacSHA256");
                    sha256_HMAC.init(new SecretKeySpec(key.getBytes(), "HmacSHA256"));
                    sha256_HMAC.update(smsText.getBytes());
                    sha256_HMAC.update(smsSender.getBytes());
                    byte[] hmac = sha256_HMAC.doFinal();

                    Intent smsIntent = new Intent(Const.PACKAGE_KP2A_PLUGIN);
                    smsIntent.setAction(Const.SMS_PROXY_ACTION_KP2A_SMS_RELAY);

                    smsIntent.putExtra(Const.SMS_PROXY_EXTRA_KP2A_KEY, key);
                    smsIntent.putExtra(Const.SMS_PROXY_EXTRA_SMS_TEXT, smsText);
                    smsIntent.putExtra(Const.SMS_PROXY_EXTRA_SMS_SENDER, smsSender);
                    smsIntent.putExtra(Const.SMS_PROXY_EXTRA_HMAC, hmac);
                    mCtx.sendBroadcast(smsIntent);
                } catch (Exception e) {
                    //toast?
                }
            }
        }
    }

    static void showNotification() {
        mNotificationBuilder = new NotificationCompat.Builder(mCtx);
        mNotificationBuilder.setContentTitle(mCtx.getString(R.string.app_name));
        mNotificationBuilder.setSmallIcon(R.drawable.ic_notification);
        mNotificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (smsReceiverRegistered) {
            mNotificationBuilder.setContentText(mCtx.getString(R.string.text_notification_active));

            Intent stopActionIntent = new Intent(mCtx, SMSService.class);
            stopActionIntent.setAction(Const.SMS_PROXY_ACTION_FORCE_STOP);
            PendingIntent stopActionPendingIntent = PendingIntent.getService(mCtx, 0, stopActionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mNotificationBuilder.addAction(0, mCtx.getString(R.string.button_stop), stopActionPendingIntent);

        } else {
            mNotificationBuilder.setContentText(mCtx.getString(R.string.text_notification_permission_error));

            Intent intent = new Intent(mCtx, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(mCtx, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT); //FLAG_ACTIVITY_NEW_TASK
            mNotificationBuilder.setContentIntent(pendingIntent);
        }

        mNotificationManager.notify(Const.NOTIFICATION_ID_SERVICE, mNotificationBuilder.build());
    }

    static void hideNotification() {
        mNotificationManager.cancel(Const.NOTIFICATION_ID_SERVICE);
    }

    static void registerSMSReceiver() {
        if (ContextCompat.checkSelfPermission(mCtx, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            IntentFilter intentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
            intentFilter.setPriority(100);
            mCtx.registerReceiver(smsReceiver, intentFilter);
            smsReceiverRegistered = true;
        }

        //always show notification if the service is running but has no sms permission
        if (prefs.getBoolean(Const.PREF_NOTIFICATION, false) || ( !smsReceiverRegistered)) {
            showNotification();
        }
    }

    static boolean isRunning() {
        return isRunning;
    }

    private void updateStopTime() {
        stopTime = System.currentTimeMillis() + TIMER_LIMIT_MS;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final String action = intent.getAction();
            if (Const.SMS_PROXY_ACTION_KEEP_ALIVE.equals(action)) {
                updateStopTime();
            }
            if (Const.SMS_PROXY_ACTION_FORCE_STOP.equals(action)) {
                stopSelf();
            }
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mCtx = this;
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(mTimerTask, TIMER_INTERVAL_MS);
        updateStopTime();

        registerSMSReceiver();

        isRunning = true;
    }


    @Override
    public void onDestroy() {
        isRunning = false;

        hideNotification();
        if (smsReceiverRegistered) {
            try {
                unregisterReceiver(smsReceiver);
                smsReceiverRegistered = false;
            } catch (Exception e) {
            }
        }

        mCtx = null;
        mNotificationManager = null;
        mNotificationBuilder = null;
        mHandler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // Auto-generated method stub
        return null;
    }

}
