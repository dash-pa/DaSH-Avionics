package org.dash.avionics.alerts;

import org.dash.avionics.alerts.CruiseSpeedCalculator.Aircraft;
import org.dash.avionics.calibration.CalibrationProfile;

public class CruiseSpeedAlerter {
  private static final float SPEED_MARGIN = 2.0f;

  private final CruiseSpeedAlertListener listener;
  private float targetSpeed;
  private boolean alerting = false;

  // Notifications are sent only once.
  public interface CruiseSpeedAlertListener {
    void onLowSpeed();

    void onHighSpeed();

    void onStoppedAlerting();
  }

  public CruiseSpeedAlerter(CruiseSpeedAlertListener listener) {
    this.listener = listener;
  }

  public void setCalibration(CalibrationProfile profile) {
    Aircraft aircraft = profile.getActiveAircraft();
    float pilotWeightKg = profile.getPilotWeight();
    targetSpeed = CruiseSpeedCalculator.getCruiseAirspeed(aircraft, pilotWeightKg);
  }

  public void updateCurrentSpeed(float speed) {
    // TODO: Do smoothing over time

    boolean alreadyAlerting = alerting;
    alerting = true;
    if (speed > targetSpeed + SPEED_MARGIN) {
      if (!alreadyAlerting) listener.onHighSpeed();
    } else if (speed < targetSpeed - SPEED_MARGIN) {
      if (!alreadyAlerting) listener.onLowSpeed();
    } else {
      alerting = false;
      if (alreadyAlerting) listener.onStoppedAlerting();
    }
  }

}
