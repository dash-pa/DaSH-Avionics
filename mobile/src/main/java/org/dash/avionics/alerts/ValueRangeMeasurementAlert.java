package org.dash.avionics.alerts;

import android.support.annotation.Nullable;

import com.google.common.collect.Range;

import org.dash.avionics.aircraft.AircraftSettings_;
import org.dash.avionics.aircraft.CruiseSpeedCalculator;
import org.dash.avionics.data.MeasurementType;

/**
 * Class to handle alerts for general aircraft measurments (height and airspeed)
 */
public class ValueRangeMeasurementAlert implements MeasurementAlert {
  private Range<Float> expectedRange;
  private MeasurementType measurementType;
  AlertType low, normal, high, unknown;
  private boolean armed = false;

  public ValueRangeMeasurementAlert(MeasurementType type,
                                    AlertType low, AlertType normal, AlertType high, AlertType unknown
  ) {
    measurementType = type;
    this.low = low;
    this.normal = normal;
    this.high = high;
    this.unknown = unknown;
  }

  @Override
  /**
   * Only Airspeed and height supported
   */
  public void updateSettings(AircraftSettings_ settings) {
    synchronized (this) {
      if (measurementType == MeasurementType.AIRSPEED) {
        float targetSpeed = CruiseSpeedCalculator.getCruiseAirspeedFromSettings(settings);
        float speedMargin = settings.getMaxSpeedDelta().get();
        expectedRange = Range.closed(targetSpeed - speedMargin, targetSpeed + speedMargin);
      } else if (measurementType == MeasurementType.HEIGHT) {
        float targetHeight = settings.getTargetHeight().get();
        float heightMargin = settings.getMaxHeightDelta().get();
        expectedRange = Range.closed(targetHeight - heightMargin, targetHeight + heightMargin);
      }
      armed = false;
    }
  }

  @Nullable
  @Override
  public AlertType updateMeasurment(MeasurementType type, Float value) {
    synchronized (this) {
      if (expectedRange == null || type != measurementType) {
        return null;
      }
      if (value == null) {
        return unknown;
      }
      //only arm an alert after it has had a "normal" reading
      if (expectedRange.contains(value)) {
        armed = true;
      }
      if (!armed) {
        return null;
      }

      if (expectedRange.contains(value)) {
        return normal;
      } else if (value <= expectedRange.lowerEndpoint()) {
        return low;
      } else {
        return high;
      }
    }
  }
}
