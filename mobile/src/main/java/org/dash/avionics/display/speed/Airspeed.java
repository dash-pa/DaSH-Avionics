package org.dash.avionics.display.speed;

import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import org.androidannotations.annotations.sharedpreferences.Pref;
import org.dash.avionics.data.model.ValueModel;
import org.dash.avionics.display.DisplayConfiguration;
import org.dash.avionics.display.widget.Widget;
import org.dash.avionics.sensors.SensorPreferences_;

/**
 * Displays airspeed according to multiple gauges
 */
class Airspeed extends Widget {
  private final Paint textPaint;
  private final float textSize;
  private final int airSpeedColor = Color.WHITE;
  private final int kpSpeedColor = Color.GRAY;
  private final int impSpeedColor = Color.DKGRAY;
  private final SpeedGauge.Model model;
  private boolean singleLine;

  public Airspeed(DisplayConfiguration config, AssetManager assets, float x, float y, float w, float h, boolean singleLineOnly, SpeedGauge.Model model) {
    super(x, y, w, h);
    singleLine = singleLineOnly;

    this.model = model;

    Typeface tf = Typeface.createFromAsset(assets, config.mTextTypeface);

    textSize = .2f * w;

    textPaint = new Paint();
    textPaint.setColor(Color.GREEN);
    textPaint.setTypeface(tf);
    textPaint.setTextSize(textSize);
    textPaint.setTextAlign(Paint.Align.RIGHT);
    textPaint.setAntiAlias(true);
  }

  @Override
  protected void drawContents(Canvas canvas) {
    ValueModel<Float> airSpeed = model.getSpeed();
    String speedText = (airSpeed.isValid() ? String.format("%3.0f", airSpeed.getValue()) : "xxx") + "kph";
    textPaint.setColor(airSpeedColor);
    canvas.drawText("  " + speedText, getX() + 0.95f * getWidth(), textSize, textPaint);

    if (singleLine) return;

    ValueModel<Float> kpSpeed = model.getKpSpeed();
    String kpText = (kpSpeed.isValid() ? String.format("%3.0f", kpSpeed.getValue()) : "xxx") + "kph";
    textPaint.setColor(kpSpeedColor);
    canvas.drawText("k " + kpText, getX() + 0.95f * getWidth(), textSize * 2, textPaint);

    ValueModel<Float> impSpeed = model.getImpSpeed();
    String impText = (impSpeed.isValid() ? String.format("%3.0f", impSpeed.getValue()) : "xxx") + "kph";
    textPaint.setColor(impSpeedColor);
    canvas.drawText("i " + impText, getX() + 0.95f * getWidth(), textSize * 3, textPaint);


  }
}
