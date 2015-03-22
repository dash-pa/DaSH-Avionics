package org.dash.avionics.display;

import android.content.res.AssetManager;
import android.content.res.Resources;

import org.dash.avionics.display.altitude.AltitudeTape;
import org.dash.avionics.display.climbrate.ClimbRateTape;
import org.dash.avionics.display.speed.SpeedTape;
import org.dash.avionics.display.widget.Container;

public class PFD extends Container {
  public PFD(Resources resources, AssetManager assets, PFDModel model,
             final float width, final float height) {
    sizeTo(width, height);

    float instrumentGap = (float) Math.floor(width / 75);
    float altitudeTapeWidth = 0.125f * width;
    float speedTapeWidth = 0.125f * width;
    float climbRateTapeWidth = 0.05f * width;
    float airballWidth =
        getWidth() - speedTapeWidth - altitudeTapeWidth - climbRateTapeWidth
            - (3 * instrumentGap);

    DisplayConfiguration config = new DisplayConfiguration(width, height, resources, assets);

    float x = 0f;
    mChildren.add(new SpeedTape(
        config, resources, assets,
        x, 0f,
        speedTapeWidth, getHeight(),
        model));
    x += speedTapeWidth + instrumentGap;
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
  }
}
