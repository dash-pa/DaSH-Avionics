package org.dash.avionics.display;

import android.content.res.AssetManager;
import android.content.res.Resources;

import com.google.common.base.Preconditions;

import org.dash.avionics.display.altitude.AltitudeTape;
import org.dash.avionics.display.climbrate.ClimbRateTape;
import org.dash.avionics.display.crank.CrankGauge;
import org.dash.avionics.display.prop.PropGauge;
import org.dash.avionics.display.speed.SpeedTape;
import org.dash.avionics.display.track.TrackDrawing;
import org.dash.avionics.display.vitals.VitalsDisplay;
import org.dash.avionics.display.widget.Container;

public class PFD extends Container {
  public PFD(Resources resources, AssetManager assets, PFDModel model,
             float width, float height) {
    sizeTo(width, height);

    float instrumentGap = (float) Math.floor(width / 65);
    float altitudeTapeWidth = 0.125f * width;
    float speedTapeWidth = 0.1f * width;
    float climbRateTapeWidth = 0.05f * width;
    float crankGaugeWidth = 0.2f * width;
    float propGaugeWidth = 0.2f * width;
    float vitalsWidth = 0.2f * width;
    float trackWidth =
        getWidth() - speedTapeWidth - altitudeTapeWidth - climbRateTapeWidth -
            Math.max(crankGaugeWidth, propGaugeWidth) - vitalsWidth - (6 * instrumentGap);
    Preconditions.checkState(trackWidth > 0);

    float crankGaugeHeight = .4f * height;
    float propGaugeHeight = .4f * height;
    float vitalsHeight = .4f * height;

    DisplayConfiguration config = new DisplayConfiguration(width, height, resources, assets);

    float x = 0f;
    mChildren.add(new SpeedTape(
        config, resources, assets,
        x, 0f,
        speedTapeWidth, getHeight(),
        model));
    x += speedTapeWidth + instrumentGap;

    mChildren.add(new PropGauge(
        config, resources, assets,
        x, 0f,
        propGaugeWidth, propGaugeHeight, model));

    mChildren.add(new CrankGauge(
        config, resources, assets,
        x, propGaugeHeight + instrumentGap,
        crankGaugeWidth, crankGaugeHeight, model));

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
    x += vitalsWidth + instrumentGap;

    mChildren.add(new AltitudeTape(
        config, resources, assets,
        x, 0f,
        altitudeTapeWidth, getHeight(),
        model));
    x += altitudeTapeWidth + instrumentGap;
    mChildren.add(new ClimbRateTape(
        config, resources, assets,
        x, 0f,
        climbRateTapeWidth, getHeight(),
        model));
  }
}
