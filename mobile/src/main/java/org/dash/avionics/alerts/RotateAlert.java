package org.dash.avionics.alerts;

import android.provider.Settings;
import android.support.annotation.Nullable;

import com.google.common.collect.Range;

import org.dash.avionics.aircraft.AircraftSettings_;
import org.dash.avionics.aircraft.CruiseSpeedCalculator;
import org.dash.avionics.data.MeasurementType;

import java.util.Set;

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
  private final long MAX_ACTIVE_TIME_MS = 2300;
  private long activeStart;

  @Override
  /**
   * ROtate is based on airpseed alone
   */
  public void updateSettings(AircraftSettings_ settings) {
    synchronized (this) {
      if (measurementType == MeasurementType.AIRSPEED) {
        float targetSpeed = settings.getRotateAirspeed().get();
        float speedMargin = settings.getMaxSpeedDelta().get();

        //Reset the alert if the airspeed drops way below target speed
        expectedRange = Range.closed((targetSpeed - speedMargin) / 2, targetSpeed);
      }

      active = false;
    }
  }

  @Override
  public void updateMeasurment(MeasurementType type, Float value, Set<AlertType> activeAlerts) {
    synchronized (this) {
      if (value == null || expectedRange == null || type != measurementType) {
        return;
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
        activeAlerts.remove(low);
        activeAlerts.add(normal);
      } else {
        activeAlerts.remove(normal);
        activeAlerts.add(low);
      }
    }
  }
}
