package org.dash.avionics.display.speed;

import android.content.res.AssetManager;
import android.content.res.Resources;

import com.google.common.base.Preconditions;

import org.dash.avionics.data.model.ValueModel;
import org.dash.avionics.display.DisplayConfiguration;
import org.dash.avionics.display.widget.Container;

public class SpeedGauge extends Container {
  private final Airspeed speed;

  public interface Model {
    //Main Weathermeter
    ValueModel<Float> getSpeed();
    //Kingpost Weathermeter
    ValueModel<Float> getKpSpeed();
    //Hall effect sensor
    ValueModel<Float> getImpSpeed();
  }

  public SpeedGauge(
          DisplayConfiguration config, Resources res, AssetManager assets,
          float x, float y, float w, float h,
          final SpeedGauge.Model model) {
    moveTo(x, y);
    sizeTo(w, h);

    float padding = .05f * h;
    float iconHeight = .4f * h;

    speed = new Airspeed(config, assets, 0, iconHeight + padding, w, h, model);
    mChildren.add(speed);
  }
}