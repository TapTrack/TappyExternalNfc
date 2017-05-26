package com.taptrack.swan;

import com.taptrack.tcmptappy.tappy.ble.TappyBleDeviceDefinition;

public interface TappySelectionListener {
    void tappySelected(TappyBleDeviceDefinition definition);
}
