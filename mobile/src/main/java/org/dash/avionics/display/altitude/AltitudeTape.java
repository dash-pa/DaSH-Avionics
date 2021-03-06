package org.dash.avionics.display.altitude;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import org.dash.avionics.R;
import org.dash.avionics.data.model.AircraftModel;
import org.dash.avionics.data.model.ValueModel;
import org.dash.avionics.display.DisplayConfiguration;
import org.dash.avionics.display.widget.Container;
import org.dash.avionics.display.widget.FilledPolygon;
import org.dash.avionics.display.widget.Rectangle;
import org.dash.avionics.display.widget.TiledImage;
import org.dash.avionics.display.widget.Widget;

public class AltitudeTape extends Container {

  public interface Model {
    ValueModel<Float> getAltitude();

    ValueModel<AircraftModel> getAircraft();
  }

  private final Model mModel;
  private final AltitudeTapeCore mTape;
  private final Widget mInvalidImage;

  /**
   * Create an AltitudeTape.
   */
  public AltitudeTape(
      DisplayConfiguration config,
      Resources res, AssetManager assets,
      float x, float y, float w, float h,
      final Model model) {
    mModel = model;

    moveTo(x, y);
    sizeTo(w, h);

    float invalidWidth = (float) Math.floor(w / 8);

    mTape = new AltitudeTapeCore(
        config, assets,
        config.mPointerToScaleOffset, 0f, w - config.mPointerToScaleOffset, h,
        new AltitudeTapeCore.Model() {
          @Override
          public float getAltitude() {
            return model.getAltitude().isValid() ? model.getAltitude().getValue() : 0f;
          }

          @Override
          public float getLowAlertAltitude() {
            return model.getAircraft().isValid() ? model.getAircraft().getValue().getMinHeight()
                : 0f;
          }

          @Override
          public float getLowWarningAltitude() {
            return model.getAircraft().isValid() ? model.getAircraft().getValue().getLowHeight()
                : 0f;
          }

          @Override
          public float getHighWarningAltitude() {
            return model.getAircraft().isValid() ? model.getAircraft().getValue().getHighHeight()
                : 0f;
          }

          @Override
          public float getHighAlertAltitude() {
            return model.getAircraft().isValid() ? model.getAircraft().getValue().getMaxHeight()
                : 0f;
          }
        });

    Widget pointerSymbol = new FilledPolygon(
        new float[][]{
            {0.0f, 0.0f},
            {1.0f, 0.5f},
            {0.0f, 1.0f},
        },
        config.mPointerColor);
    pointerSymbol.sizeTo(config.mPointerShapeSize, config.mPointerShapeSize);

    Widget pointerLine = new Rectangle(config.mPointerColor);
    pointerLine.sizeTo(w, config.mThickLineThickness);

    pointerSymbol.moveTo(
        0, (getHeight() - pointerSymbol.getHeight()) / 2f);
    pointerLine.moveTo(
        0, (getHeight() - pointerLine.getHeight()) / 2f);

    mInvalidImage = new TiledImage(BitmapFactory.decodeResource(res, R.mipmap.error_texture));
    mInvalidImage.moveTo(config.mPointerToScaleOffset, 0f);
    mInvalidImage.sizeTo(invalidWidth, h);

    mChildren.add(mTape);
    mChildren.add(mInvalidImage);
    mChildren.add(pointerSymbol);
    mChildren.add(pointerLine);
  }

  @Override
  protected void drawContents(Canvas canvas) {
    if (!mModel.getAltitude().isValid()) {
      mInvalidImage.setVisible(true);
      mTape.setVisible(false);
    } else {
      mInvalidImage.setVisible(false);
      mTape.setVisible(true);
    }
    super.drawContents(canvas);
  }
}
