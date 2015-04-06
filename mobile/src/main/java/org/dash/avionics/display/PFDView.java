package org.dash.avionics.display;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewTreeObserver;

import com.google.common.base.Preconditions;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EView;
import org.androidannotations.api.BackgroundExecutor;

@EView
public class PFDView extends SurfaceView {

  private PFD widget;

  private class FrameRateCounter {
    private static final int REPORTING_INTERVAL = 100;
    private int mCount;
    private long mTime;

    public void addRenderingTime(long time) {
      mCount++;
      mTime += time;
      if (mCount == REPORTING_INTERVAL) {
        double frameRate = (double) mCount / (double) mTime * 1000.0;
        Log.v(PFDView.class.getName(), "Frame rate = " + frameRate);
        mCount = 0;
        mTime = 0L;
      }
    }
  }

  private final FrameRateCounter mFrameRateCounter = new FrameRateCounter();
  private PFDModel mModel;

  public PFDView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);

    this.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            initialize();
            getViewTreeObserver().removeOnGlobalLayoutListener(this);
          }
        });
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public PFDView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  public void setModel(final PFDModel model) {
    Preconditions.checkState(mModel == null);

    mModel = model;
  }

  private void initialize() {
    Preconditions.checkState(mModel != null);

    widget = new PFD(getResources(), getContext().getAssets(), mModel,
        getWidth(), getHeight());
    final Runnable drawCallback = new Runnable() {
      @Override
      public void run() {
        draw();
      }
    };

    getHolder().addCallback(new SurfaceHolder.Callback() {
      @Override
      public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
      }

      @Override
      public void surfaceCreated(SurfaceHolder holder) {
        // Force a first draw (likely only with invalid data).
        draw();

        // Subscribe to future data updates.
        mModel.addUpdateListener(drawCallback);

        periodicRedraw();
      }

      @Override
      public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v("PFDView", "Surface destroyed");
        BackgroundExecutor.cancelAll("watchdog", true);
        mModel.removeUpdateListener(drawCallback);
      }
    });
  }

  private synchronized void draw() {
    Canvas canvas = getHolder().lockCanvas();
    if (canvas != null) {
      canvas.drawColor(Color.BLACK);
      long start = System.currentTimeMillis();
      widget.draw(canvas);
      mFrameRateCounter.addRenderingTime(System.currentTimeMillis() - start);
      getHolder().unlockCanvasAndPost(canvas);
    }
  }

  @SuppressWarnings("InfiniteRecursion")
  @Background(id = "watchdog", delay = 200)
  void periodicRedraw() {
    // Ensure a redraw even if we get no data.
    draw();
    periodicRedraw();
  }
}
