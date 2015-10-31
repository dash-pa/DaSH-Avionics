package org.dash.avionics.display.prop;

import android.content.res.AssetManager;
import android.content.res.Resources;

import com.google.common.base.Preconditions;

import org.dash.avionics.data.model.ValueModel;
import org.dash.avionics.display.DisplayConfiguration;
import org.dash.avionics.display.widget.Container;

/**
 * Propeller gauge indicating rpm.
 */
public class PropGauge extends Container {
  private final PropIcon icon;
  private final PropRpm rpm;

  public interface Model {
    ValueModel<Float> getPropRpm();
  }

  public PropGauge(
      DisplayConfiguration config, Resources res, AssetManager assets,
      float x, float y, float w, float h,
      final Model model) {
    moveTo(x, y);
    sizeTo(w, h);

    float padding = .05f * h;
    float iconHeight = .4f * h;
    float rpmHeight = .25f * h;
    Preconditions.checkState(padding + iconHeight + rpmHeight <= h);

    float currentX = .05f * w;
    float currentY = padding;
    icon = new PropIcon(config);
    icon.moveTo(.25f * w, currentY);
    icon.sizeTo(.5f * w, iconHeight);
    mChildren.add(icon);
    currentY += iconHeight + padding;

    rpm = new PropRpm(config, assets, currentX, currentY, 0.9f * w, rpmHeight, model);
    mChildren.add(rpm);
    currentY += rpmHeight;
  }
}

