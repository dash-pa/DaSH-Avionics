package org.dash.avionics.display.speed;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import org.dash.avionics.R;
import org.dash.avionics.display.DisplayConfiguration;
import org.dash.avionics.display.model.ValueModel;
import org.dash.avionics.display.widget.Container;
import org.dash.avionics.display.widget.FilledPolygon;
import org.dash.avionics.display.widget.Rectangle;
import org.dash.avionics.display.widget.TiledImage;
import org.dash.avionics.display.widget.Widget;
import org.schmivits.airball.airdata.Aircraft;

public class SpeedTape extends Container {

  public interface Model {
    ValueModel<Float> getSpeed();

    ValueModel<Aircraft> getAircraft();
  }

  private final Model mModel;
  private final Widget mTape;
  private final Widget mInvalidImage;

  /**
   * Create a SpeedTape.
   */
  public SpeedTape(
      DisplayConfiguration config, Resources res, AssetManager assets,
      float x, float y, float w, float h,
      final Model model) {
    moveTo(x, y);
    sizeTo(w, h);

    mModel = model;

    float invalidWidth = (float) Math.floor(w / 8);

    mTape = new SpeedTapeCore(
        config,
        assets,
        0, 0,
        w - config.mPointerToScaleOffset, h,
        new SpeedTapeCore.Model() {
          // TODO: Get rid of this.
          @Override
          public float getSpeed() {
            return model.getSpeed().isValid() ? model.getSpeed().getValue() : 0f;
          }

          @Override
          public float getVs0() {
            return model.getAircraft().isValid() ? model.getAircraft().getValue()
                .getVs0() : 0f;
          }

          @Override
          public float getVfe() {
            return model.getAircraft().isValid() ? model.getAircraft().getValue()
                .getVfe() : 0f;
          }

          @Override
          public float getVs1() {
            return model.getAircraft().isValid() ? model.getAircraft().getValue()
                .getVs1() : 0f;
          }

          @Override
          public float getVno() {
            return model.getAircraft().isValid() ? model.getAircraft().getValue()
                .getVno() : 0f;
          }

          @Override
          public float getVne() {
            return model.getAircraft().isValid() ? model.getAircraft().getValue()
                .getVne() : 0f;
          }
        });

    Widget pointerSymbol = new FilledPolygon(
        new float[][]{
            {1.0f, 0.0f},
            {0.0f, 0.5f},
            {1.0f, 1.0f},
        },
        config.mPointerColor);
    pointerSymbol.moveTo(0f, 0f);
    pointerSymbol.sizeTo(config.mPointerShapeSize, config.mPointerShapeSize);

    Widget pointerLine = new Rectangle(config.mPointerColor);
    pointerLine.moveTo(0f, 0f);
    pointerLine.sizeTo(w, config.mThickLineThickness);

    pointerSymbol.moveTo(
        getWidth() - pointerSymbol.getWidth(),
        (getHeight() - pointerSymbol.getHeight()) / 2f);
    pointerLine.moveTo(
        getWidth() - pointerLine.getWidth(), (getHeight() - pointerLine.getHeight()) / 2f);

    mInvalidImage = new TiledImage(BitmapFactory.decodeResource(res, R.mipmap.error_texture));
    mInvalidImage.moveTo(getWidth() - invalidWidth - config.mPointerToScaleOffset, 0f);
    mInvalidImage.sizeTo(invalidWidth, h);

    mInvalidImage.setVisible(false);
    mChildren.add(mTape);
    mChildren.add(mInvalidImage);
    mChildren.add(pointerSymbol);
    mChildren.add(pointerLine);
  }

  @Override
  protected void drawContents(Canvas canvas) {
    if (!mModel.getSpeed().isValid() || !mModel.getAircraft().isValid()) {
      mInvalidImage.setVisible(true);
      mTape.setVisible(false);
    } else {
      mInvalidImage.setVisible(false);
      mTape.setVisible(true);
    }
    super.drawContents(canvas);
  }
}
