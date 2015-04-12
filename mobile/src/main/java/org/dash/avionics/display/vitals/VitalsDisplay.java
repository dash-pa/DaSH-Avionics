package org.dash.avionics.display.vitals;

import android.content.res.AssetManager;
import android.content.res.Resources;

import com.google.common.base.Preconditions;

import org.dash.avionics.data.model.ValueModel;
import org.dash.avionics.display.DisplayConfiguration;
import org.dash.avionics.display.widget.Container;

/**
 * Vitals display indicating heartbeat rate.
 */
public class VitalsDisplay extends Container {
  private final VitalsIcon icon;
  private final HeartRate hr;

  public interface Model {
    ValueModel<Float> getHeartRate();
  }

  public VitalsDisplay(
      DisplayConfiguration config, Resources res, AssetManager assets,
      float x, float y, float w, float h,
      final Model model) {
    moveTo(x, y);
    sizeTo(w, h);

    float padding = .05f * h;
    float iconHeight = .4f * h;
    float hrHeight = .25f * h;
    Preconditions.checkState(padding + iconHeight + hrHeight <= h);

    float currentY = padding;
    icon = new VitalsIcon(config);
    icon.moveTo(.25f * w, currentY);
    icon.sizeTo(.5f * w, iconHeight);
    mChildren.add(icon);
    currentY += iconHeight + padding;

    hr = new HeartRate(config, assets, 0.05f * w, currentY, 0.9f * w, hrHeight);
    mChildren.add(hr);
    currentY += hrHeight;
  }
}

