package org.dash.avionics.display;

import android.content.res.AssetManager;
import android.content.res.Resources;

import org.dash.avionics.display.crank.CrankGauge;
import org.dash.avionics.display.speed.SpeedGauge;
import org.dash.avionics.display.widget.Container;

/**
 * PFD center display which shows only the crank gauge.
 */
public class PFDCenter2 extends Container {

  PFDCenter2(DisplayConfiguration config, Resources resources, AssetManager assets, PFDModel model,
             float x, float width, float height) {
    moveTo(x, 0);
    sizeTo(width, height);

    float instrumentGap = (float) Math.floor(width / 60);

    x = 0f;
    mChildren.add(new CrankGauge(
        config, resources, assets,
        x, 0f,
        width, height, true, model));

    mChildren.add(new SpeedGauge(
            config, resources, assets,
            x, instrumentGap,
            width, height, true, model));
  }
}
