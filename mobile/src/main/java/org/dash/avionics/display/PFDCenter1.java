package org.dash.avionics.display;

import android.content.res.AssetManager;
import android.content.res.Resources;

import com.google.common.base.Preconditions;

import org.dash.avionics.display.crank.CrankGauge;
import org.dash.avionics.display.prop.PropGauge;
import org.dash.avionics.display.speed.SpeedGauge;
import org.dash.avionics.display.track.TrackDrawing;
import org.dash.avionics.display.vitals.VitalsDisplay;
import org.dash.avionics.display.widget.Container;

/**
 * PFD center instruments displaying prop RPM, pedal RPM/power, track and heart rate.
 */
public class PFDCenter1 extends Container {

  PFDCenter1(DisplayConfiguration config, Resources resources, AssetManager assets, PFDModel model,
             float x, float width, float height) {
    moveTo(x, 0);
    sizeTo(width, height);

    float instrumentGap = (float) Math.floor(width / 60);

    float crankGaugeWidth = 0.25f * width;
    float airSpeedWidth = 0.25f * width;
    float propGaugeWidth = 0.25f * width;
    float vitalsWidth = 0.25f * width;
    float trackWidth =
        width - Math.max(crankGaugeWidth, propGaugeWidth) - vitalsWidth - (2 * instrumentGap);
    Preconditions.checkState(trackWidth > 0);

    float crankGaugeHeight = .4f * height;
    float propGaugeHeight = .4f * height;
    float vitalsHeight = .4f * height;
    float airspeedHeight = .4f * height;

    x = 0f;
    mChildren.add(new PropGauge(
        config, resources, assets,
        x, 0f,
        propGaugeWidth, propGaugeHeight, model));

    mChildren.add(new CrankGauge(
        config, resources, assets,
        x, propGaugeHeight + instrumentGap,
        crankGaugeWidth, crankGaugeHeight, false, model));

    x += Math.max(crankGaugeWidth, propGaugeWidth) + instrumentGap;

    // TODO: Track drawing should be the bottommost layer.
    mChildren.add(new TrackDrawing(
        config, resources, assets,
        x, 0f,
        trackWidth, height, model));
    x += trackWidth + instrumentGap;

    mChildren.add(new VitalsDisplay(
        config, resources, assets,
        x, 0f,
        vitalsWidth, vitalsHeight, model));

    mChildren.add(new SpeedGauge(
        config, resources, assets,
        x, vitalsHeight + instrumentGap,
        airSpeedWidth, airspeedHeight, model));
  }
}
