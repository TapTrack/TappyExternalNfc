package com.taptrack.swan;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TappyControlView extends RecyclerView {
    @NonNull
    private TappyControlViewState state;
    @NonNull
    private TappyControlAdapter adapter;

    private final Comparator<TappyWithStatus> tappySorter = new Comparator<TappyWithStatus>() {
        @Override
        public int compare(TappyWithStatus o1, TappyWithStatus o2) {
            return o1.getDeviceDefinition().getName().compareTo(o2.getDeviceDefinition().getName());
        }
    };

    public TappyControlView(Context context) {
        super(context);
        initTappyControlView(context);
    }

    public TappyControlView(Context context,
                            @Nullable AttributeSet attrs) {
        super(context, attrs);
        initTappyControlView(context);
    }

    public TappyControlView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initTappyControlView(context);
    }

    private void initTappyControlView(Context context) {
        state = new TappyControlViewState(Collections.<TappyWithStatus>emptyList());
        setLayoutManager(new LinearLayoutManager(context));
        adapter = new TappyControlAdapter();
        setAdapter(adapter);
        reset();
    }

    public void setViewState(@NonNull TappyControlViewState newState) {
        state = newState;
        reset();
    }

    public void setTappyControlListener(@Nullable TappyControlListener listener) {
        adapter.setTappyControlListener(listener);
    }

    public void clearTappyControlListener() {
        adapter.setTappyControlListener(null);
    }

    private void reset() {
        Collection<TappyWithStatus> tappyCollection = state.getTappies();
        List<TappyWithStatus> sortedList = new ArrayList<>(tappyCollection);
        Collections.sort(sortedList, tappySorter);
        adapter.setTappies(sortedList);
    }
}
