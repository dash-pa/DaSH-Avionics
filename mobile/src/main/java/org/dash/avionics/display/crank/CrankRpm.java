package org.dash.avionics.display.crank;

import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import org.dash.avionics.display.DisplayConfiguration;
import org.dash.avionics.display.widget.Widget;

/**
 * Displays the crank RPM value.
 */
class CrankRpm extends Widget {
  private static final float TEXT_BASELINE_TO_CENTER_FACTOR = 0.375f;

  private final Paint textPaint;
  private final float textSize;

  public CrankRpm(DisplayConfiguration config, AssetManager assets, float x, float y, float w, float h) {
    super(x, y, w, h);

    Typeface tf = Typeface.createFromAsset(assets, config.mTextTypeface);

    textSize = .25f * w;

    textPaint = new Paint();
    textPaint.setColor(config.mTextColor);
    textPaint.setTypeface(tf);
    textPaint.setTextSize(textSize);
    textPaint.setTextAlign(Paint.Align.RIGHT);
    textPaint.setAntiAlias(true);
  }

  @Override
  protected void drawContents(Canvas canvas) {
    canvas.drawText("1000rpm", getX() + 0.95f * getWidth(), textSize, textPaint);
  }
}
