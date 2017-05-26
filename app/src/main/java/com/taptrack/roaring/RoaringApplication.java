package com.taptrack.roaring;

import android.app.Application;
import android.os.HandlerThread;
import android.os.Looper;

import timber.log.Timber;

public class RoaringApplication extends Application {
    public static final String ACTION_TAG_FOUND = "com.taptrack.roaring.action.TAG_FOUND";
    public static final String ACTION_NDEF_FOUND = "com.taptrack.roaring.action.NDEF_FOUND";
    public static final String EXTRA_TAG_TYPE_INT = "com.taptrack.roaring.extra.TAG_TYPE";

    private RoaringApplicationState state;

    private HandlerThread stateThread;
    private Looper stateLooper;

    @Override
    public void onCreate() {
        super.onCreate();
        stateThread = new HandlerThread("STATE_THREAD");
        stateThread.start();
        stateLooper = stateThread.getLooper();

        state = new RoaringApplicationState(this,stateLooper);

        if(BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    public RoaringApplicationState getApplicationState() {
        return state;
    }
}
