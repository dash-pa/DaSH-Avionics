package org.dash.avionics.display.prop;

import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import org.dash.avionics.display.DisplayConfiguration;
import org.dash.avionics.display.widget.Widget;

/**
 * Displays the propeller RPM value.
 */
class PropRpm extends Widget {
  private final Paint textPaint;
  private final float textSize;

  public PropRpm(DisplayConfiguration config, AssetManager assets, float x, float y, float w, float h) {
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
