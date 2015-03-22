package org.dash.avionics.display.airball;

import org.dash.avionics.display.DisplayConfiguration;
import org.dash.avionics.display.widget.Widget;

public abstract class TotemPoleDecoration extends Widget {

    protected final DisplayConfiguration mConfig;
    protected float mUnitSize;

    protected TotemPoleDecoration(DisplayConfiguration config) {
        mConfig = config;
        setClip(false);
    }

    public void setUnitSize(float unitSize) {
        this.mUnitSize = unitSize;
        computeSize();
    }

    protected abstract void computeSize();
}