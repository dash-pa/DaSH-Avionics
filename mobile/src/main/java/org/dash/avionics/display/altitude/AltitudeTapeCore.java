package org.dash.avionics.display.altitude;

import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;

import org.dash.avionics.display.DisplayConfiguration;
import org.dash.avionics.display.widget.Widget;

public class AltitudeTapeCore extends Widget {
  private static final int GREEN_ARC_COLOR = Color.GREEN;
  private static final int YELLOW_ARC_COLOR = Color.YELLOW;
  private static final int RED_ARC_COLOR = Color.RED;

  public interface Model {
    float getAltitude();
    float getLowAlertAltitude();
    float getLowWarningAltitude();
    float getHighWarningAltitude();
    float getHighAlertAltitude();
  }

  private final DisplayConfiguration mConfig;
  private final Model mModel;
  private final Paint mTextTensPaint = new Paint();
  private final Paint mTextWholePaint = new Paint();
  private final Paint mGreenArcPaint = new Paint();
  private final Paint mYellowArcPaint = new Paint();
  private final Paint mRedArcPaint = new Paint();

  private final float mArcBoundaryFromLeft;
  private final float mArcThickness;
  private final float mTapePixelsPerFoot;
  private final float mTextTensSize;
  private final float mTextWholeSize;
  private final float mTickMarkFifthLength;
  private final float mTickMarkWholeLength;
  private final float mTickMarkTensLength;
  private final float mTensEmphasisLineDistanceFromText;
  private final float mTensEmphasisLineDistanceFromLeft;
  private final float mTextTensRightBoundary;
  private final float mTextWholeLeftBoundary;

  public AltitudeTapeCore(
      DisplayConfiguration config, AssetManager assets,
      float x, float y, float w, float h,
      Model model) {
    super(x, y, w, h);
    mConfig = config;
    mModel = model;

    mTapePixelsPerFoot = (float) Math.floor(w);

    mTextTensSize = (float) Math.floor(w / 3.25);
    mTextWholeSize = (float) Math.floor(w / 4.0);

    mTickMarkFifthLength = (float) Math.floor(w / 8.5);
    mTickMarkWholeLength = (float) Math.floor(mTickMarkFifthLength * 1.5);
    mTickMarkTensLength = (float) Math.floor(mTickMarkFifthLength * 2);

    mTensEmphasisLineDistanceFromText = (float) Math.floor(mTextTensSize / 10);
    mTensEmphasisLineDistanceFromLeft = (float) Math.floor(mTickMarkFifthLength * 2.5);

    mTextTensRightBoundary = (float) Math.floor(
        mTensEmphasisLineDistanceFromLeft + mTextTensSize * 1.0);
    mTextWholeLeftBoundary = mTextTensRightBoundary + (float) Math.floor(mTextTensSize / 10);

    mArcBoundaryFromLeft = mConfig.mThinLineThickness * 2.0f;
    mArcThickness = mTickMarkFifthLength - 2.0f * mConfig.mThinLineThickness;

    Typeface tf = Typeface.createFromAsset(assets, mConfig.mTextTypeface);

    mTextTensPaint.setColor(mConfig.mTextColor);
    mTextTensPaint.setTypeface(tf);
    mTextTensPaint.setTextSize(mTextTensSize);
    mTextTensPaint.setTextAlign(Align.RIGHT);
    mTextTensPaint.setAntiAlias(true);

    mTextWholePaint.setColor(mConfig.mTextColor);
    mTextWholePaint.setTypeface(tf);
    mTextWholePaint.setTextSize(mTextWholeSize);
    mTextWholePaint.setTextAlign(Align.LEFT);
    mTextWholePaint.setAntiAlias(true);

    mGreenArcPaint.setColor(GREEN_ARC_COLOR);
    mYellowArcPaint.setColor(YELLOW_ARC_COLOR);
    mRedArcPaint.setColor(RED_ARC_COLOR);
  }

  @Override
  protected void drawContents(Canvas canvas) {
    drawScaleLine(canvas);
    drawArcs(canvas);

    // Loop through 0.2m increments above and below the midpoint.
    float midpointFeet = (getHeight() / 2f) / mTapePixelsPerFoot;
    int alt02AtTop = (int) Math.ceil((mModel.getAltitude() + midpointFeet) / 0.2) + 1;
    int alt02AtBottom = (int) Math.floor((mModel.getAltitude() - midpointFeet) / 0.2) - 1;
    for (int i = alt02AtBottom; i <= alt02AtTop; i++) {
      drawAltitude(canvas, i * 0.2f);
    }
  }

  private void drawArcs(Canvas canvas) {
    canvas.drawRect(
        mArcBoundaryFromLeft,
        0.0f,
        mArcBoundaryFromLeft + mArcThickness,
        altitudeToCanvasPosition(mModel.getHighAlertAltitude()),
        mRedArcPaint);
    canvas.drawRect(
        mArcBoundaryFromLeft,
        altitudeToCanvasPosition(mModel.getHighAlertAltitude()),
        mArcBoundaryFromLeft + mArcThickness,
        altitudeToCanvasPosition(mModel.getHighWarningAltitude()),
        mYellowArcPaint);
    canvas.drawRect(
        mArcBoundaryFromLeft,
        altitudeToCanvasPosition(mModel.getHighWarningAltitude()),
        mArcBoundaryFromLeft + mArcThickness,
        altitudeToCanvasPosition(mModel.getLowWarningAltitude()),
        mGreenArcPaint);
    canvas.drawRect(
        mArcBoundaryFromLeft,
        altitudeToCanvasPosition(mModel.getLowWarningAltitude()),
        mArcBoundaryFromLeft + mArcThickness,
        altitudeToCanvasPosition(mModel.getLowAlertAltitude()),
        mYellowArcPaint);
    canvas.drawRect(
        mArcBoundaryFromLeft,
        altitudeToCanvasPosition(mModel.getLowAlertAltitude()),
        mArcBoundaryFromLeft + mArcThickness,
        altitudeToCanvasPosition(0.0f),
        mRedArcPaint);

  }

  private void drawScaleLine(Canvas canvas) {
    float yMax = Math.min(getHeight(), altitudeToCanvasPosition(0f));
    canvas.drawRect(0, 0, mConfig.mThinLineThickness, yMax + mConfig.mVeryThickLineThickness / 2, mConfig.mLinePaint);
  }

  private void drawAltitude(Canvas canvas, float altitude) {
    if (altitude < 0) { return; }

    boolean isWhole = ((int)altitude) == altitude;
    boolean isTens = ((altitude % 10f) == 0);

    float y = altitudeToCanvasPosition(altitude);
    if (isTens || isWhole) {
      int tens = (int) Math.floor(altitude / 10);
      int whole = ((int) Math.floor(altitude)) % 10;

      String tensString = tens == 0 ? "" : "" + tens;
      String wholeString = whole + ".0";
      canvas.drawText(tensString, mTextTensRightBoundary, y + 0.35f * mTextTensSize,
          mTextTensPaint);
      canvas.drawText(wholeString, mTextWholeLeftBoundary, y + 0.35f * mTextWholeSize,
          mTextWholePaint);

      if (isTens) {
        canvas.drawRect(
            0, y - mConfig.mVeryThickLineThickness / 2,
            mTickMarkTensLength, y + mConfig.mVeryThickLineThickness / 2,
            mConfig.mLinePaint);
        canvas.drawRect(
            mTensEmphasisLineDistanceFromLeft,
            y - mTextTensSize / 2 - mTensEmphasisLineDistanceFromText - mConfig.mThinLineThickness,
            getWidth(),
            y - mTextTensSize / 2 - mTensEmphasisLineDistanceFromText,
            mConfig.mLinePaint);
        canvas.drawRect(
            mTensEmphasisLineDistanceFromLeft,
            y + mTextTensSize / 2 + mTensEmphasisLineDistanceFromText,
            getWidth(),
            y + mTextTensSize / 2 + mTensEmphasisLineDistanceFromText + mConfig.mThinLineThickness,
            mConfig.mLinePaint);
      } else if (isWhole) {
        canvas.drawRect(
            0, y - mConfig.mThickLineThickness / 2,
            mTickMarkWholeLength, y + mConfig.mThickLineThickness / 2,
            mConfig.mLinePaint);
      }
    } else {
      canvas.drawRect(
          0, y - mConfig.mThinLineThickness / 2,
          mTickMarkFifthLength, y + mConfig.mThinLineThickness / 2,
          mConfig.mLinePaint);
    }
  }

  private float altitudeToCanvasPosition(float alt) {
    return (getHeight() / 2f) - (alt - mModel.getAltitude()) * mTapePixelsPerFoot;
  }
}
