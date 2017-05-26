package com.taptrack.roaring;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.taptrack.tcmptappy.tappy.ble.TappyBleDeviceDefinition;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import timber.log.Timber;

public class TappyService extends Service implements RoaringApplicationState.StateChangedListener,
        TappyBleManagementDelegate.DeviceStatusListener,
        TappyBleManagementDelegate.TagScanListener {
    private static final int NOTIFICATION_ID = 23424;
    private static final int URL_LAUNCH_THROTTLE_MS = 500; //millseconds

    RoaringApplicationState appState;

    private final HashMap<TappyBleDeviceDefinition,TappyBleManagementDelegate> currentManagers = new HashMap<>();

    private  long lastTimeLaunched = 0;
    private  String lastUrlLaunched = null;

    private Set<TappyBleDeviceDefinition> lastActiveDevices = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appState = ((RoaringApplication) getApplicationContext()).getApplicationState();
        appState.addListener(this,true);
    }

    @Override
    public int onStartCommand(Intent intent,
                              int flags,
                              int startId) {

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        appState.removeListener(this);
        synchronized (currentManagers) {
            // if the current managers contain more than one item when onDestroy is called,
            // something funny is happening
            if(currentManagers.size() != 0) {
                for (TappyBleManagementDelegate delegate : currentManagers.values()) {
                    delegate.close(true);
                }
                currentManagers.clear();
            }

            appState.reqClearStatuses();
        }
    }

    protected void resetDevices() {
        Set<TappyBleDeviceDefinition> desiredActiveDevices = appState.currentActiveDevices();
        Set<TappyBleDeviceDefinition> failedDevices = new HashSet<>();
        synchronized (currentManagers) {
            Set<TappyBleDeviceDefinition> currentDevices = currentManagers.keySet();
            Set<TappyBleDeviceDefinition> newDevices = new HashSet<>(desiredActiveDevices);
            newDevices.removeAll(currentDevices);

            Set<TappyBleDeviceDefinition> removedDevices = new HashSet<>(currentDevices);
            removedDevices.removeAll(desiredActiveDevices);

            Set<TappyBleDeviceDefinition> keptDevices = new HashSet<>(currentDevices);
            keptDevices.removeAll(removedDevices);

            for(TappyBleDeviceDefinition removedDevice : removedDevices) {
                TappyBleManagementDelegate delegate = currentManagers.get(removedDevice);
                delegate.close(false);
            }

            for(TappyBleDeviceDefinition newDevice : newDevices) {
                TappyBleManagementDelegate manager = new TappyBleManagementDelegate(newDevice,this,this,this);
                currentManagers.put(newDevice,manager);
                if(!manager.activate()) {
                    Timber.w("Device failed: "+newDevice.getName());
                    failedDevices.add(newDevice);
                }
            }

            // This takes into account when you attempt a reconnect
            // to a Tappy you previously disconnected from
            for(TappyBleDeviceDefinition keptDevice : keptDevices) {
                TappyBleManagementDelegate delegate = currentManagers.get(keptDevice);
                if(delegate.hasStartedClosing()) {
                    delegate.goSilent();

                    TappyBleManagementDelegate manager = new TappyBleManagementDelegate(keptDevice,this,this,this);
                    currentManagers.put(keptDevice,manager);
                    if(!manager.activate()) {
                        Timber.w("Device failed: "+keptDevice.getName());
                        failedDevices.add(keptDevice);
                    }
                }
            }
        }
        appState.reqRemoveActiveDevices(failedDevices);

        if(desiredActiveDevices.size() > 0) {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this,0,notificationIntent,0);

            String notificationTitle;
            String notificationContent;
            if(desiredActiveDevices.size() == 1) {
                notificationTitle = "Active Tappies";
                notificationContent = "One Tappy active"+(appState.shouldLaunchUrls() ? " with URL launching enabled" : "");
            } else {
                notificationTitle = "Active Tappies";
                notificationContent = String.format("%d Tappies active",desiredActiveDevices.size())+(appState.shouldLaunchUrls() ? " with URL launching enabled" : "");
            }

            Notification notification = new Notification.Builder(this)
                    .setContentTitle(notificationTitle)
                    .setContentText(notificationContent)
                    .setSmallIcon(R.drawable.ic_tappy_connected_notification)
                    .setContentIntent(pendingIntent)
                    .setTicker(notificationContent)
                    .build();
            startForeground(NOTIFICATION_ID, notification);
        } else {
            stopForeground(true);
            stopSelf();
        }
    }

    @Override
    public void onStateChanged(RoaringApplicationState state) {
        this.appState = state;
        Set<TappyBleDeviceDefinition> newDevices = this.appState.currentActiveDevices();
        if(!newDevices.equals(lastActiveDevices)) {
            lastActiveDevices = newDevices;
            resetDevices();
        }
    }

    @Override
    public void newDeviceStatus(TappyBleDeviceDefinition deviceDefinition, int status) {
        if(this.appState != null) {
            this.appState.reqSetTappyState(deviceDefinition,status);
        }
    }

    protected void broadcastCompat(Intent intent) {
        PackageManager pm= getPackageManager();
        List<ResolveInfo> matches=pm.queryBroadcastReceivers(intent, 0);

        for (ResolveInfo resolveInfo : matches) {
            Intent explicit=new Intent(intent);
            ComponentName cn=
                    new ComponentName(resolveInfo.activityInfo.applicationInfo.packageName,
                            resolveInfo.activityInfo.name);

            explicit.setComponent(cn);
            sendBroadcast(explicit);
        }
    }

    @Override
    public void tagFound(byte[] uid, byte tagType) {
        Intent intent = new Intent(RoaringApplication.ACTION_TAG_FOUND);
        intent.putExtra(NfcAdapter.EXTRA_ID,uid);
        intent.putExtra(RoaringApplication.EXTRA_TAG_TYPE_INT,tagType);

        broadcastCompat(intent);
    }

    @Override
    public void ndefFound(byte[] uid, byte tagType, NdefMessage message) {
        if(this.appState != null && this.appState.shouldLaunchUrls()) {
            throttleAndLaunch(message);
        }

        Intent intent = new Intent(RoaringApplication.ACTION_NDEF_FOUND);
        intent.putExtra(NfcAdapter.EXTRA_ID,uid);
        intent.putExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, new NdefMessage[]{message});
        intent.putExtra(RoaringApplication.EXTRA_TAG_TYPE_INT,tagType);

        broadcastCompat(intent);
    }

    protected void throttleAndLaunch(NdefMessage message) {
        NdefRecord[] records = message.getRecords();
        if (records.length != 0) {
            NdefRecord firstRecord = records[0];
            if (firstRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN &&
                    Arrays.equals(firstRecord.getType(), NdefRecord.RTD_URI)) {
                byte[] uriPayload = firstRecord.getPayload();
                if (uriPayload.length > 1) {
                    byte prefixByte = uriPayload[0];
                    String url = null;
                    switch (prefixByte) {
                    case 0x01:
                        url = "http://www." + new String(Arrays.copyOfRange(uriPayload, 1, uriPayload.length));
                        break;
                    case 0x02:
                        url = "https://www." + new String(Arrays.copyOfRange(uriPayload, 1, uriPayload.length));
                        break;
                    case 0x03:
                        url = "http://" + new String(Arrays.copyOfRange(uriPayload, 1, uriPayload.length));
                        break;
                    case 0x04:
                        url = "https://" + new String(Arrays.copyOfRange(uriPayload, 1, uriPayload.length));
                        break;
                    }
                    if (url != null) {
                        long currentTime = System.currentTimeMillis();
                        if (!url.equals(lastUrlLaunched) || (lastTimeLaunched + URL_LAUNCH_THROTTLE_MS) < currentTime) {
                            Intent launchUrlIntent = new Intent(Intent.ACTION_VIEW);
                            launchUrlIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            launchUrlIntent.setData(Uri.parse(url));
                            startActivity(launchUrlIntent);

                            lastUrlLaunched = url;
                            lastTimeLaunched = currentTime;
                        }
                    }
                }
            }
        }
    }
}
