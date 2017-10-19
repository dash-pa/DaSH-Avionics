package org.dash.avionics.alerts;

import android.provider.Settings;
import android.support.annotation.Nullable;

import com.google.common.collect.Range;

import org.dash.avionics.aircraft.AircraftSettings_;
import org.dash.avionics.aircraft.CruiseSpeedCalculator;
import org.dash.avionics.data.MeasurementType;

/**
 * Class to handle alerts for general aircraft measurments (height and airspeed)
 */
public class RotateAlert implements MeasurementAlert {
  private Range<Float> expectedRange;
  private final MeasurementType measurementType = MeasurementType.AIRSPEED;
  private final AlertType low = AlertType.LOW_ROTATE, normal = AlertType.NORMAL_ROTATE,
          high = AlertType.HIGH_ROTATE, unknown = AlertType.UNKNOWN_ROTATE;
  private boolean armed = true, active = false;
  //Alarm should only be active this long
  private final long MAX_ACTIVE_TIME_MS = 4000;
  private long activeStart;

  @Override
  /**
   * Only Airspeed and height supported
   */
  public void updateSettings(AircraftSettings_ settings) {
    synchronized (this) {
      if (measurementType == MeasurementType.AIRSPEED) {
        float targetSpeed = settings.getRotateAirspeed().get();
        float speedMargin = settings.getMaxSpeedDelta().get();

        expectedRange = Range.closed(targetSpeed, targetSpeed + speedMargin);
      }

      active = false;
    }
  }

  @Nullable
  @Override
  public AlertType updateMeasurment(MeasurementType type, Float value) {
    synchronized (this) {
      if (value == null || expectedRange == null || type != measurementType) {
        return null;
      }

      if (value < expectedRange.lowerEndpoint()) {
          armed = true;
          active = false;
      }
      if (value > expectedRange.upperEndpoint() && armed) {
          armed = false;
          active = true;
          activeStart = System.currentTimeMillis();
      }

      if (active && (System.currentTimeMillis() - activeStart > MAX_ACTIVE_TIME_MS)) {
        active = false;
      }

      if (active) {
        return normal;
      }

      return low;
    }
  }
}
