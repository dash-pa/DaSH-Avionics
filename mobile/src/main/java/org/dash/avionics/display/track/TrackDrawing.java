package org.dash.avionics.display.track;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.GeomagneticField;
import android.location.Location;

import org.dash.avionics.data.model.ValueModel;
import org.dash.avionics.display.DisplayConfiguration;
import org.dash.avionics.display.widget.Widget;

import java.util.List;

/**
 * Draws the track with recent positions.
 */
public class TrackDrawing extends Widget {

  private static final Paint PATH_PAINT = new Paint();
  private static final Paint NORTH_PAINT = new Paint();
  static {
    PATH_PAINT.setColor(Color.RED);
    PATH_PAINT.setAntiAlias(true);
    PATH_PAINT.setStrokeWidth(1.0f/700.0f);
    PATH_PAINT.setStyle(Paint.Style.STROKE);
    NORTH_PAINT.setColor(Color.GREEN);
    NORTH_PAINT.setAntiAlias(true);
    NORTH_PAINT.setStrokeWidth(1.0f/1000.0f);
    NORTH_PAINT.setStyle(Paint.Style.STROKE);
  }

  private final Model model;

  public interface Model {
    List<Location> getLocationHistory();
    ValueModel<Float> getHeading();
  }

  public TrackDrawing(DisplayConfiguration config, Resources res, AssetManager assets,
                      float x, float y, float w, float h,
                      final Model model) {
    this.model = model;

    moveTo(x, y);
    sizeTo(w, h);
    setDrawBounds(true);
  }

  @Override
  protected void drawContents(Canvas canvas) {
    List<Location> locationHistory = model.getLocationHistory();
    if (locationHistory == null || locationHistory.isEmpty()) {
//      Log.d("Track", "No history");
      return;
    }

    Location lastLocation = locationHistory.get(locationHistory.size() - 1);
    float lastX = (float) lastLocation.getLongitude();
    float lastY = (float) lastLocation.getLatitude();

    // Draw an arrow towards true north.
    Path northPath = new Path();
    northPath.moveTo(lastX, lastY);
    northPath.lineTo(lastX, lastY + 0.025f);  // North
    northPath.lineTo(lastX + 0.005f, lastY + 0.02f);
    northPath.moveTo(lastX, lastY + 0.025f);
    northPath.lineTo(lastX - 0.005f, lastY + 0.02f);

    // Now make it point towards magnetic north.
    GeomagneticField field = new GeomagneticField(lastY, lastX,
        (float) lastLocation.getAltitude(), lastLocation.getTime());
    Matrix magnetic = new Matrix();
    magnetic.setRotate(-field.getDeclination(), lastX, lastY);
    northPath.transform(magnetic);

    Path path = new Path();

    // Draw circle around current position.
    path.addCircle(lastX, lastY, 0.002f, Path.Direction.CW);

    // Draw the track.
    path.moveTo(lastX, lastY);
    for (int i = locationHistory.size() - 2; i >= 0; i--) {
      Location loc = locationHistory.get(i);
      path.lineTo((float) loc.getLongitude(), (float) loc.getLatitude());
    }

    // Center the end of the track at the top.
    canvas.translate(getWidth() / 2.0f, getHeight() * 0.2f);
    canvas.translate(-lastX, -lastY);

    // Make our current bearing be "up".
    canvas.rotate(-lastLocation.getBearing(), lastX, lastY);

    // Scale.
    float scale = canvas.getHeight() * 5f;
    canvas.scale(scale, -scale, lastX, lastY);

    // Draw.
    canvas.drawPath(northPath, NORTH_PAINT);
    canvas.drawPath(path, PATH_PAINT);
  }
}
