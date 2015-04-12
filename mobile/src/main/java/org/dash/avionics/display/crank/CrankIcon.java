package org.dash.avionics.display.crank;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;

import org.dash.avionics.display.DisplayConfiguration;
import org.dash.avionics.display.widget.Widget;

import java.io.IOException;

/**
 * An icon of a crank.
 *
 * TODO: Color the crank depending on RPM, using a Paint color filter.
 */
class CrankIcon extends Widget {
  private static final String CRANK_IMAGE_FILE = "img/crank.png";

  private final Bitmap mBitmap;

  CrankIcon(DisplayConfiguration config) {
    try {
      mBitmap = BitmapFactory.decodeStream(config.mAssets.open(CRANK_IMAGE_FILE));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void drawContents(Canvas canvas) {
    canvas.drawBitmap(mBitmap, null, new RectF(0f, 0f, getWidth(), getHeight()), null);
  }
}
