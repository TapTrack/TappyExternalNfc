package com.taptrack.swan;

import com.taptrack.tcmptappy.tappy.ble.TappyBleDeviceDefinition;
import com.taptrack.tcmptappy.tappy.ble.TappyBleDeviceStatus;

public class TappyWithStatus {

    private final TappyBleDeviceDefinition deviceDefinition;
    private final int status;

    public TappyWithStatus(TappyBleDeviceDefinition deviceDefinition, int status) {
        this.deviceDefinition = deviceDefinition;
        this.status = status;
    }

    public TappyBleDeviceDefinition getDeviceDefinition() {
        return deviceDefinition;
    }

    public int getStatus() {
        return status;
    }

    public boolean isReady() {
        return status == TappyBleDeviceStatus.READY;
    }

    public boolean isConnecting() {
        return status == TappyBleDeviceStatus.CONNECTING;
    }

    public boolean isConnected() {
        return status == TappyBleDeviceStatus.CONNECTED;
    }

    public boolean isDisconencting() {
        return status == TappyBleDeviceStatus.DISCONNECTING;
    }

    public boolean isDisconnected() {
        return status == TappyBleDeviceStatus.DISCONNECTED;
    }

    public boolean isInErrorState() {
        return status == TappyBleDeviceStatus.ERROR;
    }

    public boolean isStatusUnknown() {
        return status == TappyBleDeviceStatus.UNKNOWN;
    }
}
