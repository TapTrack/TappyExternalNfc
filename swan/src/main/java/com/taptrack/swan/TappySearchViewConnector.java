package com.taptrack.swan;

import android.support.annotation.NonNull;

import com.taptrack.tcmptappy.tappy.ble.TappyBleDeviceDefinition;
import com.taptrack.tcmptappy.tappy.ble.scanner.TappyBleFoundListener;
import com.taptrack.tcmptappy.tappy.ble.scanner.TappyBleScanner;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TappySearchViewConnector {
    private final TappySearchView view;
    private final TappyBleScanner scanner;
    private final TappyBleFoundListener listener = new TappyBleFoundListener() {
        @Override
        public void onTappyBleFound(TappyBleDeviceDefinition tappyBle) {
            tappiesFound.add(tappyBle);
            notifyView();
        }
    };
    @NonNull
    private final Set<TappyBleDeviceDefinition> tappiesFound;

    private boolean shouldBeScanning;
    private boolean isScanning;

    public TappySearchViewConnector(TappySearchView view) {
        this(view, Collections.<TappyBleDeviceDefinition>emptySet());
    }

    public TappySearchViewConnector(TappySearchView view, Set<TappyBleDeviceDefinition> currentDevices) {
        this.view = view;
        tappiesFound = new HashSet<>(currentDevices);
        scanner = TappyBleScanner.get();
        scanner.registerTappyBleFoundListener(listener);
        notifyView();
    }

    @NonNull
    public Set<TappyBleDeviceDefinition> getFoundTappies() {
        return tappiesFound;
    }

    public void clearResults() {
        tappiesFound.clear();
        notifyView();
    }

    public void setShouldScan(boolean shouldBeScanning) {
        this.shouldBeScanning = shouldBeScanning;
        reset();
    }

    protected void notifyView() {
        this.view.post(new Runnable() {
            @Override
            public void run() {
                view.setTappySearchState(new TappySearchState(tappiesFound));
            }
        });
    }

    protected void reset() {
        if(shouldBeScanning) {
            start();
        } else {
            stop();
        }
    }

    protected void start() {
        if(!isScanning) {
            scanner.startScan();
            isScanning = true;
        }
    }

    protected void stop() {
        if(isScanning) {
            scanner.stopScan();
            isScanning = false;
        }
    }

    public void onPause() {
        stop();
    }

    public void onResume() {
        reset();
    }
}
