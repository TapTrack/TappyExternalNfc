package com.taptrack.swan;

import com.taptrack.tcmptappy.tappy.ble.TappyBleDeviceDefinition;

import java.util.Set;

public class TappySearchState {
    private Set<TappyBleDeviceDefinition> devices;

    public TappySearchState(Set<TappyBleDeviceDefinition> devices) {
        this.devices = devices;
    }

    public Set<TappyBleDeviceDefinition> getDevices() {
        return devices;
    }
}
