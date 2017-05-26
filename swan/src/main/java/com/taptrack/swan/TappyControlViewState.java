package com.taptrack.swan;

import java.util.Collection;

public class TappyControlViewState {
    private final Collection<TappyWithStatus> tappies;

    public TappyControlViewState(Collection<TappyWithStatus> tappies) {
        this.tappies = tappies;
    }

    public Collection<TappyWithStatus> getTappies() {
        return tappies;
    }
}
