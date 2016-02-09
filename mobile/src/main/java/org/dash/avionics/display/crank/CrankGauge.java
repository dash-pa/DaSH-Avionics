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
    float powerHeight;
    float powerWidth;
    float currentY = 0f;

    if (largeRpm) {
      padding = .03f * h;
      rpmHeight = .8f * h;
      powerHeight = .12f * h;
      powerWidth = .4f * w;

      currentY += padding;
    } else {
      padding = .05f * h;
      rpmHeight = .25f * h;
      powerHeight = .2f * h;
      powerWidth = 0.9f * w;

      currentY += padding;

      float iconHeight = .4f * h;
      float iconWidth = .5f * w;

      CrankIcon icon = new CrankIcon(config);
      icon.moveTo(centerX(w, iconWidth), currentY);
      icon.sizeTo(iconWidth, iconHeight);
      mChildren.add(icon);
      currentY += iconHeight + padding;
    }

    float totalHeight = currentY + rpmHeight + padding + powerHeight;
    Preconditions.checkState(totalHeight <= h, "height=" + totalHeight + "; max=" + h);

    rpm = new CrankRpm(config, assets, centerX(w, 0.9f*w), currentY, 0.9f * w, rpmHeight, largeRpm,
        model);
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

