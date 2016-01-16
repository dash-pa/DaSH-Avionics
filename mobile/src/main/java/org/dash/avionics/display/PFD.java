package org.dash.avionics.display;

import android.content.res.AssetManager;
import android.content.res.Resources;

import com.google.common.collect.Lists;

import org.dash.avionics.display.altitude.AltitudeTape;
import org.dash.avionics.display.climbrate.ClimbRateTape;
import org.dash.avionics.display.speed.SpeedTape;
import org.dash.avionics.display.widget.Container;
import org.dash.avionics.display.widget.Widget;

import java.util.List;

public class PFD extends Container {
  private final List<Widget> centers = Lists.newArrayList();

  public PFD(Resources resources, AssetManager assets, PFDModel model,
             float width, float height) {
    sizeTo(width, height);

    float instrumentGap = (float) Math.floor(width / 65);
    float altitudeTapeWidth = 0.125f * width;
    float speedTapeWidth = 0.1f * width;
    float climbRateTapeWidth = 0.05f * width;
    float centerWidth = getWidth() - speedTapeWidth - altitudeTapeWidth - climbRateTapeWidth;

    DisplayConfiguration config = new DisplayConfiguration(width, height, resources, assets);

    float x = 0f;
    mChildren.add(new SpeedTape(
        config, resources, assets,
        x, 0f,
        speedTapeWidth, getHeight(),
        model));
    x += speedTapeWidth + instrumentGap;

    centers.add(new PFDCenter1(config, resources, assets, model, x, centerWidth, height));
    mChildren.add(centers.get(0));
    x += centerWidth;

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

  public void onPFDClicked() {
    // TODO: Switch center.
  }
}
