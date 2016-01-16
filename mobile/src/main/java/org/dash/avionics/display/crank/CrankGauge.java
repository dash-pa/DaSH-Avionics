package org.dash.avionics.display.crank;

import android.content.res.AssetManager;
import android.content.res.Resources;

import com.google.common.base.Preconditions;

import org.dash.avionics.data.model.ValueModel;
import org.dash.avionics.display.DisplayConfiguration;
import org.dash.avionics.display.widget.Container;

/**
 * Crank gauge indicating power and rpm.
 */
public class CrankGauge extends Container {
  private final CrankIcon icon;
  private final CrankRpm rpm;
  private final CrankPower power;

  public interface Model {
    ValueModel<Float> getCrankRpm();
    ValueModel<Float> getCrankPower();
  }

  public CrankGauge(
      DisplayConfiguration config, Resources res, AssetManager assets,
      float x, float y, float w, float h, boolean largeRpm,
      final Model model) {
    moveTo(x, y);
    sizeTo(w, h);

    float padding;
    float rpmHeight;
    float iconHeight;
    float iconWidth;
    float powerHeight;
    float powerWidth;
    if (largeRpm) {
      padding = .03f * h;
      rpmHeight = .4f * h;
      iconHeight = .24f * h;
      iconWidth = .2f * w;
      powerHeight = .12f * h;
      powerWidth = 0.4f * w;
    } else {
      padding = .05f * h;
      rpmHeight = .25f * h;
      iconHeight = .4f * h;
      iconWidth = .5f * w;
      powerHeight = .25f * h;
      powerWidth = 0.9f * w;
    }
    Preconditions.checkState(2 * padding + iconHeight + rpmHeight + powerHeight <= h);

    float currentY = padding;
    icon = new CrankIcon(config);
    icon.moveTo(centerX(w, iconWidth), currentY);
    icon.sizeTo(iconWidth, iconHeight);
    mChildren.add(icon);
    currentY += iconHeight + padding;

    rpm = new CrankRpm(config, assets, centerX(w, 0.9f*w), currentY, 0.9f * w, rpmHeight, model);
    mChildren.add(rpm);
    currentY += rpmHeight + padding;

    power = new CrankPower(config, assets, centerX(w, powerWidth), currentY, powerWidth, powerHeight,
        model);
    mChildren.add(power);
    currentY += powerHeight;
  }

  private static float centerX(float totalWidth, float widgetWidth) {
    return (.5f * totalWidth) - (widgetWidth / 2f);
  }
}

