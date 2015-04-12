package org.dash.avionics.display.prop;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;

import org.dash.avionics.display.DisplayConfiguration;
import org.dash.avionics.display.widget.Widget;

import java.io.IOException;

/**
 * An icon of a propeller.
 *
 * TODO: Color the prop depending on RPM, using a Paint color filter.
 */
class PropIcon extends Widget {
  private static final String PROPELLER_IMAGE_FILE = "img/propeller.png";

  private final Bitmap mBitmap;

  PropIcon(DisplayConfiguration config) {
    try {
      mBitmap = BitmapFactory.decodeStream(config.mAssets.open(PROPELLER_IMAGE_FILE));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void drawContents(Canvas canvas) {
    canvas.drawBitmap(mBitmap, null, new RectF(0f, 0f, getWidth(), getHeight()), null);
  }
}
