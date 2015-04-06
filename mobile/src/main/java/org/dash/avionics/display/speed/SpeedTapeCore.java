package org.dash.avionics.display.speed;

import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;

import org.dash.avionics.display.DisplayConfiguration;
import org.dash.avionics.display.widget.Widget;

public class SpeedTapeCore extends Widget {

  public interface Model {
    float getSpeed();
    float getVs0();
    float getVs1();  // Bottom of green arc
    float getVno();  // Top of green / bottom of yellow arc
    float getVne();  // Top of yellow / bottom of red arc

  }

  // The below constants, while they may seem silly ;), are useful for
  // tweaking exact RGB values if necessary
  private static final int GREEN_ARC_COLOR = Color.GREEN;
  private static final int YELLOW_ARC_COLOR = Color.YELLOW;
  private static final int RED_ARC_COLOR = Color.RED;

  private static final float TEXT_BASELINE_TO_CENTER_FACTOR = 0.375f;

  private final DisplayConfiguration mConfig;
  private final Model mModel;

  private final float mVArcRightBoundaryFromRight;
  private final float mTapePixelsPerKnot;
  private final float mTickMarkFivesLength;
  private final float mTickMarkTensLength;
  private final float mTextSize;
  private final float mTextRightBoundaryFromRight;
  private final float mVArcThickness;
  private final Paint mTextPaint = new Paint();
  private final Paint mGreenArcPaint = new Paint();
  private final Paint mYellowArcPaint = new Paint();
  private final Paint mRedArcPaint = new Paint();

  public SpeedTapeCore(
      DisplayConfiguration config,
      AssetManager assets, 
      float x, float y, float w, float h,
      Model model) {
    super(x, y, w, h);

    mConfig = config;
    mModel = model;

    mVArcThickness = (float) Math.floor(w / 12);
    mVArcRightBoundaryFromRight = 2f * mConfig.mThinLineThickness;
    mTapePixelsPerKnot = (float) Math.floor(w * 1.5);
    mTickMarkFivesLength = (float) Math.floor(2 * mVArcThickness);
    mTickMarkTensLength = (float) Math.floor(2.25 * mVArcThickness);
    mTextSize = (float) Math.floor(w / 1.5);
    mTextRightBoundaryFromRight = (float) Math.floor(2.75 * mVArcThickness);

    Typeface tf = Typeface.createFromAsset(assets, mConfig.mTextTypeface);

    mTextPaint.setColor(mConfig.mTextColor);
    mTextPaint.setTypeface(tf);
    mTextPaint.setTextSize(mTextSize);
    mTextPaint.setTextAlign(Align.RIGHT);
    mTextPaint.setAntiAlias(true);

    mGreenArcPaint.setColor(GREEN_ARC_COLOR);
    mYellowArcPaint.setColor(YELLOW_ARC_COLOR);
    mRedArcPaint.setColor(RED_ARC_COLOR);
  }

  @Override
  protected void drawContents(Canvas canvas) {
    drawScaleLine(canvas);
    drawVArcs(canvas);
    float midPointKnots = getHeight() / 2 / mTapePixelsPerKnot;
    int speed25AtTop = (int)
        Math.floor((mModel.getSpeed() - midPointKnots) / .25) - 1;
    int speed25AtBottom = (int)
        Math.ceil((mModel.getSpeed() + midPointKnots) / .25) + 1;
    for (int i = speed25AtTop; i <= speed25AtBottom; i++) {
      drawSpeed(canvas, i * .25f);
    }
  }

  private void drawScaleLine(Canvas canvas) {
    float yMin = Math.max(0, speedToCanvasPosition(0f));
    canvas.drawRect(
        getWidth() - mConfig.mThinLineThickness, yMin - mConfig.mThinLineThickness / 2,
        getWidth(), getHeight(),
        mConfig.mLinePaint);
  }

  private void drawVArcs(Canvas canvas) {
    canvas.drawRect(
        getWidth() - mVArcRightBoundaryFromRight - mVArcThickness, speedToCanvasPosition(0),
        getWidth() - mVArcRightBoundaryFromRight, speedToCanvasPosition(mModel.getVs0()),
        mRedArcPaint);
    canvas.drawRect(
        getWidth() - mVArcRightBoundaryFromRight - mVArcThickness, speedToCanvasPosition(mModel.getVs0()),
        getWidth() - mVArcRightBoundaryFromRight, speedToCanvasPosition(mModel.getVs1()),
        mYellowArcPaint);
    canvas.drawRect(
        getWidth() - mVArcRightBoundaryFromRight - mVArcThickness, speedToCanvasPosition(mModel.getVs1()),
        getWidth() - mVArcRightBoundaryFromRight, speedToCanvasPosition(mModel.getVno()),
        mGreenArcPaint);
    canvas.drawRect(
        getWidth() - mVArcRightBoundaryFromRight - mVArcThickness, speedToCanvasPosition(mModel.getVno()),
        getWidth() - mVArcRightBoundaryFromRight, speedToCanvasPosition(mModel.getVne()),
        mYellowArcPaint);
    canvas.drawRect(
        getWidth() - mVArcRightBoundaryFromRight - mVArcThickness, speedToCanvasPosition(mModel.getVne()),
        getWidth() - mVArcRightBoundaryFromRight, getHeight(),
        mRedArcPaint);
  }

  private void drawSpeed(Canvas canvas, float speed) {
    if (speed < 0) { return; }

    float y = speedToCanvasPosition(speed);
    boolean isTens = ((int)speed) == speed;

    if (isTens) {
      canvas.drawText(
          String.format("%.0f", speed),
          getWidth() - mTextRightBoundaryFromRight, y + mTextSize * TEXT_BASELINE_TO_CENTER_FACTOR,
          mTextPaint);
      canvas.drawRect(
          getWidth() - mTickMarkTensLength, y - mConfig.mThickLineThickness / 2,
          getWidth(), y + mConfig.mThickLineThickness / 2,
          mConfig.mLinePaint);
    } else {
      canvas.drawRect(
          getWidth() - mTickMarkFivesLength, y - mConfig.mThinLineThickness / 2,
          getWidth(), y + mConfig.mThinLineThickness / 2,
          mConfig.mLinePaint);
    }
  }

  float speedToCanvasPosition(float speed) {
    return getHeight() / 2 + (speed - mModel.getSpeed()) * mTapePixelsPerKnot;
  }
}
