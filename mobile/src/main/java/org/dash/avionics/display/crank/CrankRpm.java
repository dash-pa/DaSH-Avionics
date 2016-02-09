package org.dash.avionics.display.crank;

import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import org.dash.avionics.data.model.ValueModel;
import org.dash.avionics.display.DisplayConfiguration;
import org.dash.avionics.display.widget.Widget;

/**
 * Displays the crank RPM value.
 */
class CrankRpm extends Widget {
  private final Paint textPaint;
  private final float textSize;
  private final CrankGauge.Model model;

  public CrankRpm(DisplayConfiguration config, AssetManager assets, float x, float y, float w, float h, boolean large, CrankGauge.Model model) {
    super(x, y, w, h);

    this.model = model;

    Typeface tf = Typeface.createFromAsset(assets, config.mTextTypeface);

    textSize = large ? .4f * h : .25f * w;

    textPaint = new Paint();
    textPaint.setColor(config.mTextColor);
    textPaint.setTypeface(tf);
    textPaint.setTextSize(textSize);
    textPaint.setTextAlign(Paint.Align.RIGHT);
    textPaint.setAntiAlias(true);
  }

  @Override
  protected void drawContents(Canvas canvas) {
    ValueModel<Float> value = model.getCrankRpm();
    String text = (value.isValid() ? String.format("%3.0f", value.getValue()) : "XXX") + "rpm";
    canvas.drawText(text, getX() + 0.95f * getWidth(), textSize, textPaint);
  }
}
