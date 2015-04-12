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
    ValueModel<Float> getCrankpower();
  }

  public CrankGauge(
      DisplayConfiguration config, Resources res, AssetManager assets,
      float x, float y, float w, float h,
      final Model model) {
    moveTo(x, y);
    sizeTo(w, h);

    float padding = .05f * h;
    float iconHeight = .4f * h;
    float rpmHeight = .25f * h;
    float powerHeight = .25f * h;
    Preconditions.checkState(padding + iconHeight + rpmHeight + powerHeight <= h);

    float currentX = .05f * w;
    float currentY = padding;
    icon = new CrankIcon(config);
    icon.moveTo(.25f * w, currentY);
    icon.sizeTo(.5f * w, iconHeight);
    mChildren.add(icon);
    currentY += iconHeight + padding;

    rpm = new CrankRpm(config, assets, currentX, currentY, 0.9f * w, rpmHeight);
    mChildren.add(rpm);
    currentY += rpmHeight;

    power = new CrankPower(config, assets, currentX, currentY, 0.9f * w, powerHeight);
    mChildren.add(power);
    currentY += powerHeight;
  }
}

