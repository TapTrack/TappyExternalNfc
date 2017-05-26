package com.taptrack.swan;

import com.taptrack.tcmptappy.tappy.ble.TappyBleDeviceDefinition;

public interface TappyControlListener {
    void requestConnect(TappyBleDeviceDefinition deviceDefinition);
    void requestRemove(TappyBleDeviceDefinition deviceDefinition);
}
