package org.dash.avionics.display.vitals;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;

import org.dash.avionics.display.DisplayConfiguration;
import org.dash.avionics.display.widget.Widget;

import java.io.IOException;

/**
 * An icon of a heart.
 *
 * TODO: Color the heart depending on HR, using a Paint color filter.
 */
class VitalsIcon extends Widget {
  private static final String HEART_IMAGE_FILE = "img/heart.png";

  private final Bitmap mBitmap;

  VitalsIcon(DisplayConfiguration config) {
    try {
      mBitmap = BitmapFactory.decodeStream(config.mAssets.open(HEART_IMAGE_FILE));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void drawContents(Canvas canvas) {
    canvas.drawBitmap(mBitmap, null, new RectF(0f, 0f, getWidth(), getHeight()), null);
  }
}
