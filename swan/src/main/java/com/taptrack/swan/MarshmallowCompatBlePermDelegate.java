/*
 * Copyright (c) 2016. Papyrus Electronics, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.taptrack.swan;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

public class MarshmallowCompatBlePermDelegate {
    @NonNull
    protected final Activity activity;
    @NonNull
    private final ResultCallbacks callbacks;
    protected boolean hasCoarsePermission = false;
    protected BluetoothAdapter bluetoothAdapter;

    protected final int requestLocationCode;
    protected final int requestBleEnableCode;

    public interface ResultCallbacks {
        void onNoBluetoothSupported();
        void onCoarseLocationDenied();
        void onBluetoothEnableDenied();
    }

    public MarshmallowCompatBlePermDelegate(@NonNull Activity activity,
                                            int requestBleEnableCode,
                                            int requestLocationCode,
                                            @NonNull ResultCallbacks callbacks) {
        this.activity = activity;
        this.requestLocationCode = requestLocationCode;
        this.requestBleEnableCode = requestBleEnableCode;
        this.callbacks = callbacks;
    }

    public void onCreate() {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            callbacks.onNoBluetoothSupported();
            return;
        }

        int permissionCheck = ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        if(permissionCheck == PackageManager.PERMISSION_GRANTED) {
            hasCoarsePermission = true;
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                activity.requestPermissions(
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        requestLocationCode);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        if(requestCode == requestLocationCode) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                hasCoarsePermission = true;
            } else {
                callbacks.onCoarseLocationDenied();
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == requestBleEnableCode && resultCode == Activity.RESULT_CANCELED) {
            callbacks.onBluetoothEnableDenied();
            return;
        }
    }

    public void onResume() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, requestBleEnableCode);
        }
    }
}
