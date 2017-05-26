package com.taptrack.roaring;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.taptrack.swan.TappyWithStatus;
import com.taptrack.tcmptappy.tappy.ble.TappyBleDeviceDefinition;
import com.taptrack.tcmptappy.tappy.ble.TappyBleDeviceStatus;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import timber.log.Timber;

public class RoaringApplicationState {
    private static final String TAG = RoaringApplicationState.class.getSimpleName();
    private static final String APP_STATE_PREFS = TAG+".APP_STATE_PREFS";
    private static final String SHOULD_LAUNCH_PREFS = TAG+".SHOULD_LAUNCH_PREFS";

    private final RoaringApplication application;
    private final SharedPreferences preferences;

    private final CopyOnWriteArraySet<StateChangedListener> listenerSet = new CopyOnWriteArraySet<>();

    private boolean shouldLaunchUrls = false;
    private Set<TappyBleDeviceDefinition> activeDevices = new HashSet<>();
    private HashMap<TappyBleDeviceDefinition,Integer> deviceStatuses = new HashMap<>();

    private final Handler mutationHandler;
    private final Lock readLock;
    private final Lock writeLock;


    public interface StateChangedListener {
        void onStateChanged(RoaringApplicationState state);
    }

    RoaringApplicationState (RoaringApplication application, Looper mutationLooper) {
        this.application = application;
        this.mutationHandler = new Handler(mutationLooper);

        ReadWriteLock rwLock = new ReentrantReadWriteLock();
        readLock = rwLock.readLock();
        writeLock = rwLock.writeLock();

        preferences = application.getSharedPreferences(APP_STATE_PREFS, Context.MODE_PRIVATE);
        shouldLaunchUrls = preferences.getBoolean(SHOULD_LAUNCH_PREFS,false);
    }

    private void postMutationRunnable(final Runnable runnable) {
        mutationHandler.post(new Runnable() {
            @Override
            public void run() {
                while(!writeLock.tryLock()) {
                    Timber.i("Waiting on contended lock");
                    try {
                        Thread.sleep(2);
                    } catch (InterruptedException ignored) {
                    }
                }
                runnable.run();
                writeLock.unlock();
                notifyListenersOfChange();
            }
        });
    }

    public void addListener(@NonNull StateChangedListener listener, boolean callImmediately) {
        listenerSet.add(listener);
        if(callImmediately) {
            listener.onStateChanged(this);
        }
    }

    public void removeListener(@NonNull StateChangedListener listener) {
        listenerSet.remove(listener);
    }

    protected void notifyListenersOfChange() {
        Timber.i("Notifying listeners");
        for(StateChangedListener listener : listenerSet) {
            listener.onStateChanged(this);
        }
        Timber.i("Finished notifying");
    }

    public boolean shouldLaunchUrls() {
        return shouldLaunchUrls;
    }

    public void reqSetShouldLaunchUrls(final boolean newState) {
        postMutationRunnable(new Runnable() {
            @Override
            public void run() {
                if (shouldLaunchUrls != newState) {
                    shouldLaunchUrls = newState;
                    preferences.edit().putBoolean(SHOULD_LAUNCH_PREFS,newState).apply();
                }
            }
        });
    }

    public void reqAddActiveDevice(@NonNull final TappyBleDeviceDefinition deviceDefinition) {
        Timber.i("Requesting add device: "+deviceDefinition.getName());
        postMutationRunnable(new Runnable() {
            @Override
            public void run() {
                activeDevices.add(deviceDefinition);
                Timber.v("Added device: "+deviceDefinition.getName());
            }
        });
    }

    public void reqRemoveActiveDevice(@NonNull final TappyBleDeviceDefinition deviceDefinition) {
        postMutationRunnable(new Runnable() {
            @Override
            public void run() {
                activeDevices.remove(deviceDefinition);
            }
        });
    }

    public void reqRemoveActiveDevices(@NonNull final Set<TappyBleDeviceDefinition> devicesToRemove) {
        postMutationRunnable(new Runnable() {
            @Override
            public void run() {
                for(TappyBleDeviceDefinition deviceDefinition : devicesToRemove) {
                    activeDevices.remove(deviceDefinition);
                }
            }
        });
    }

    public void reqSetTappyState(final TappyBleDeviceDefinition device, final int status) {
        Timber.v(String.format("Requesting set state %s: %d",device.getName(),status));
        postMutationRunnable(new Runnable() {
            @Override
            public void run() {
                if(status != TappyBleDeviceStatus.UNKNOWN) {
                    deviceStatuses.put(device, status);
                } else {
                    deviceStatuses.remove(device);
                }
            }
        });
    }

    public void reqClearStatuses() {
        Timber.v("Requesting clear of device statuses");
        postMutationRunnable(new Runnable() {
            @Override
            public void run() {
                if(deviceStatuses.size() > 0) {
                    deviceStatuses.clear();
                }
                Timber.v("Did clear devices");
            }
        });
    }

    public Set<TappyBleDeviceDefinition> currentActiveDevices() {
        Timber.v("Requesting active devices");
        Set<TappyBleDeviceDefinition> devices;
        readLock.lock();
        devices = Collections.unmodifiableSet(new HashSet<>(activeDevices));
        readLock.unlock();
        return devices;
    }

    public Set<TappyWithStatus> currentActiveDeviceStatuses() {
        Timber.v("Requesting device statuses");

        Set<TappyWithStatus> statuses = new HashSet<>();
        readLock.lock();
        for(TappyBleDeviceDefinition deviceDefinition : activeDevices) {
            Integer status = deviceStatuses.get(deviceDefinition);
            if(status == null) {
                status = TappyBleDeviceStatus.UNKNOWN;
            }

            statuses.add(new TappyWithStatus(deviceDefinition,status));
        }
        readLock.unlock();
        Timber.v("Returning device statuses");
        return statuses;
    }

}
