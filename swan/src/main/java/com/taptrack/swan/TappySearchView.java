package com.taptrack.swan;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import com.taptrack.tcmptappy.tappy.ble.TappyBleDeviceDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TappySearchView extends RecyclerView {
    @NonNull
    private TappySearchAdapter adapter;
    @NonNull
    private TappySearchState searchState;

    private Comparator<TappyBleDeviceDefinition> deviceSorter = new Comparator<TappyBleDeviceDefinition>() {
        @Override
        public int compare(TappyBleDeviceDefinition o1, TappyBleDeviceDefinition o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };

    public TappySearchView(Context context) {
        super(context);
        initTappySearchView(context);
    }

    public TappySearchView(Context context,
                           @Nullable AttributeSet attrs) {
        super(context, attrs);
        initTappySearchView(context);
    }

    public TappySearchView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initTappySearchView(context);
    }

    private void initTappySearchView(Context context) {
        setLayoutManager(new LinearLayoutManager(context));
        adapter = new TappySearchAdapter();
        setAdapter(adapter);
        searchState = new TappySearchState(Collections.<TappyBleDeviceDefinition>emptySet());
    }

    public void setTappySearchState(@NonNull TappySearchState searchState) {
        this.searchState = searchState;
        reset();
    }

    public void setTappySelectionListener(@Nullable TappySelectionListener listener) {
        adapter.setTappySelectionListener(listener);
    }

    public void removeTappySelectionListener() {
        adapter.setTappySelectionListener(null);
    }

    protected void reset() {
        List<TappyBleDeviceDefinition> deviceList = new ArrayList<>(searchState.getDevices());
        Collections.sort(deviceList, deviceSorter);
        adapter.setDevices(deviceList);
    }
}
