package com.taptrack.roaring;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.taptrack.swan.MarshmallowCompatBlePermDelegate;
import com.taptrack.swan.TappyControlListener;
import com.taptrack.swan.TappyControlView;
import com.taptrack.swan.TappyControlViewState;
import com.taptrack.swan.TappySearchView;
import com.taptrack.swan.TappySearchViewConnector;
import com.taptrack.swan.TappySelectionListener;
import com.taptrack.swan.TappyWithStatus;
import com.taptrack.tcmptappy.tappy.ble.ParcelableTappyBleDeviceDefinition;
import com.taptrack.tcmptappy.tappy.ble.TappyBleDeviceDefinition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import timber.log.Timber;

import static com.taptrack.roaring.R.string.scan;

public class MainActivity extends AppCompatActivity
        implements RoaringApplicationState.StateChangedListener {
    private static final String TAG = MainActivity.class.getName();

    private static final int REQUEST_BLE_CODE = 1231;
    private static final int ENABLE_BLE_CODE = 891213;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private final MarshmallowCompatBlePermDelegate.ResultCallbacks resultCallbacks = new MarshmallowCompatBlePermDelegate.ResultCallbacks() {
        @Override
        public void onNoBluetoothSupported() {
            Toast.makeText(MainActivity.this, R.string.bluetooth_must_be_supported, Toast.LENGTH_SHORT).show();
            MainActivity.this.finish();
        }

        @Override
        public void onCoarseLocationDenied() {
            Toast.makeText(MainActivity.this, R.string.coarse_location_needed, Toast.LENGTH_SHORT).show();
            MainActivity.this.finish();
        }

        @Override
        public void onBluetoothEnableDenied() {
            Toast.makeText(MainActivity.this, R.string.bluetooth_must_be_enabled, Toast.LENGTH_SHORT).show();
            MainActivity.this.finish();
        }
    };

    private final MarshmallowCompatBlePermDelegate delegate =
            new MarshmallowCompatBlePermDelegate(this,REQUEST_BLE_CODE,ENABLE_BLE_CODE,resultCallbacks);

    private TappyControlView tappyControlView;
    private TappySearchView searchView;

    private AppCompatButton scanButton;
    private SwitchCompat launchUrlSwitch;
    private TappySearchViewConnector scanConnector;

    private static final String EXTRA_SHOULD_SCAN = TAG+".SHOULD_SCAN";
    private boolean shouldScan;

    private static final String EXTRA_NEARBY_TAPPIES = TAG+".NEARBY_TAPPIES";

    private RoaringApplicationState appState;
    private ProgressBar searchingIndicatorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        delegate.onCreate();

        launchUrlSwitch = (SwitchCompat) findViewById(R.id.swc_launch_urls);
        scanButton = (AppCompatButton) findViewById(R.id.bt_activate_scan);
        tappyControlView = (TappyControlView) findViewById(R.id.tcv_tappy_control);
        searchView = (TappySearchView) findViewById(R.id.tsv_tappy_search_view);
        searchingIndicatorView = (ProgressBar) findViewById(R.id.pb_search_indicator);

        tappyControlView.setTappyControlListener(new TappyControlListener() {
            @Override
            public void requestConnect(TappyBleDeviceDefinition deviceDefinition) {

            }

            @Override
            public void requestRemove(TappyBleDeviceDefinition deviceDefinition) {
                if(appState != null) {
                    appState.reqRemoveActiveDevice(deviceDefinition);
                }
            }
        });

        searchView.setTappySelectionListener(new TappySelectionListener() {
            @Override
            public void tappySelected(TappyBleDeviceDefinition definition) {
                if(appState != null) {
                    appState.reqAddActiveDevice(definition);
                }
            }
        });

        if(savedInstanceState != null) {
            ArrayList<ParcelableTappyBleDeviceDefinition> items = savedInstanceState.getParcelableArrayList(EXTRA_NEARBY_TAPPIES);
            if(items == null || items.size() == 0) {
                scanConnector = new TappySearchViewConnector(searchView);
            } else {
                scanConnector = new TappySearchViewConnector(searchView, new HashSet<TappyBleDeviceDefinition>(items));
            }
        } else {
            scanConnector = new TappySearchViewConnector(searchView);
        }

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleScanState();
            }
        });
        launchUrlSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(appState != null) {
                    appState.reqSetShouldLaunchUrls(isChecked);
                }
            }
        });

        if(savedInstanceState != null) {
            shouldScan = savedInstanceState.getBoolean(EXTRA_SHOULD_SCAN,true);
        }

        appState = ((RoaringApplication) getApplicationContext()).getApplicationState();

        resetScanner();
        resetView();
    }

    protected void toggleScanState() {
        setScanState(!shouldScan);
    }

    protected void setScanState(boolean newScanState) {
        if(newScanState != shouldScan) {
            if(newScanState) {
                scanConnector.clearResults();
            }
            shouldScan = newScanState;
            resetScanner();
            resetView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        delegate.onResume();
        scanConnector.onResume();
        appState.addListener(this,true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanConnector.onPause();
        appState.removeListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    protected void resetView() {
        Log.i(TAG,"Main activity view reset");
        if(shouldScan) {
            scanButton.setText(R.string.stop_scanning);
            searchingIndicatorView.setVisibility(View.VISIBLE);
        } else {
            scanButton.setText(scan);
            searchingIndicatorView.setVisibility(View.INVISIBLE);
        }

        launchUrlSwitch.setChecked(appState.shouldLaunchUrls());

        Set<TappyWithStatus> statuses = appState.currentActiveDeviceStatuses();

        for(TappyWithStatus status : statuses) {
            Timber.v(String.format("Tappy %s has status %d", status.getDeviceDefinition().getName(), status.getStatus()));
        }

        tappyControlView.setViewState(
                new TappyControlViewState(appState.currentActiveDeviceStatuses()));
    }

    protected void resetScanner() {
        scanConnector.setShouldScan(shouldScan);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        delegate.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        delegate.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode,resultCode,data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Set<TappyBleDeviceDefinition> tappySet = scanConnector.getFoundTappies();
        ArrayList<Parcelable> tappyList = new ArrayList<>(tappySet.size());
        for(TappyBleDeviceDefinition tappy : tappySet) {
            if(tappy instanceof Parcelable) {
                tappyList.add((Parcelable) tappy);
            } else {
                tappyList.add(new ParcelableTappyBleDeviceDefinition(tappy));
            }
        }

        outState.putParcelableArrayList(EXTRA_NEARBY_TAPPIES, tappyList);
        outState.putBoolean(EXTRA_SHOULD_SCAN, shouldScan);
    }

    @Override
    public void onStateChanged(final RoaringApplicationState state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG,"Received new app state");
                MainActivity.this.appState = state;

                Set<TappyBleDeviceDefinition> activeDevices = state.currentActiveDevices();
                if(activeDevices.size() > 0) {
                    Intent kickServiceIntent = new Intent(MainActivity.this,TappyService.class);
                    startService(kickServiceIntent);
                }
                resetView();
            }
        });
    }
}
