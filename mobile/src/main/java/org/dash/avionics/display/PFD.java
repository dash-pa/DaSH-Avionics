package org.dash.avionics.display;

import android.content.res.AssetManager;
import android.content.res.Resources;

import com.google.common.base.Preconditions;

import org.dash.avionics.display.altitude.AltitudeTape;
import org.dash.avionics.display.climbrate.ClimbRateTape;
import org.dash.avionics.display.crank.CrankGauge;
import org.dash.avionics.display.prop.PropGauge;
import org.dash.avionics.display.speed.SpeedTape;
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
    float airballWidth =
        getWidth() - speedTapeWidth - altitudeTapeWidth - climbRateTapeWidth - crankGaugeWidth -
            propGaugeWidth - (5 * instrumentGap);
    Preconditions.checkState(airballWidth > 0);

    float crankGaugeHeight = .4f * height;
    float propGaugeHeight = .4f * height;

    DisplayConfiguration config = new DisplayConfiguration(width, height, resources, assets);

    float x = 0f;
    mChildren.add(new SpeedTape(
        config, resources, assets,
        x, 0f,
        speedTapeWidth, getHeight(),
        model));
    x += speedTapeWidth + instrumentGap;

    mChildren.add(new CrankGauge(
        config, resources, assets,
        x, 0f,
        crankGaugeWidth, crankGaugeHeight, model));
    x += crankGaugeWidth + instrumentGap;

    mChildren.add(new PropGauge(
        config, resources, assets,
        x, 0f,
        propGaugeWidth, propGaugeHeight, model));
    x += propGaugeWidth + instrumentGap;

    // Skip non-existant airball
    x += airballWidth + instrumentGap;

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

    setDrawAllBounds(true);
  }
}
