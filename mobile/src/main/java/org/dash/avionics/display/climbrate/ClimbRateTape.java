package org.dash.avionics.display.climbrate;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import org.dash.avionics.R;
import org.dash.avionics.display.DisplayConfiguration;
import org.dash.avionics.display.model.ValueModel;
import org.dash.avionics.display.widget.Container;
import org.dash.avionics.display.widget.TiledImage;
import org.dash.avionics.display.widget.Widget;

public class ClimbRateTape extends Container {

    public interface Model {
        ValueModel<Float> getClimbRate();
    }

    private final Model mModel;
    private final Widget mInvalidImage;
    private final Widget mClimbRateTapeCore;

    public ClimbRateTape(DisplayConfiguration config, Resources res, AssetManager assets, float x,
            float y, float w, float h, final Model model) {
        moveTo(x, y);
        sizeTo(w, h);

        float invalidWidth = (float) Math.floor(w / 8);

        mModel = model;

        mInvalidImage = new TiledImage(BitmapFactory.decodeResource(res, R.mipmap.error_texture));
        mInvalidImage.moveTo(0f, 0f);
        mInvalidImage.sizeTo(invalidWidth, getHeight());

        mClimbRateTapeCore = new ClimbRateTapeCore(config, res, assets, 0, 0, w, h,
                new ClimbRateTapeCore.Model() {
                    @Override
                    public float getClimbRate() {
                        return model.getClimbRate().isValid() ? model.getClimbRate().getValue()
                                : 0f;
                    }
                });

        mChildren.add(mInvalidImage);
        mChildren.add(mClimbRateTapeCore);
    }

    @Override
    protected void drawContents(Canvas canvas) {
        if (!mModel.getClimbRate().isValid()) {
            mInvalidImage.setVisible(true);
            mClimbRateTapeCore.setVisible(false);
        } else {
            mInvalidImage.setVisible(false);
            mClimbRateTapeCore.setVisible(true);
        }
        super.drawContents(canvas);
    }
}
