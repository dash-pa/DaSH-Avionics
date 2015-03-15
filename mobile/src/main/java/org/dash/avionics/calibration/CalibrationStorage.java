package org.dash.avionics.calibration;

import org.androidannotations.annotations.sharedpreferences.DefaultFloat;
import org.androidannotations.annotations.sharedpreferences.DefaultInt;
import org.androidannotations.annotations.sharedpreferences.SharedPref;
import org.dash.avionics.alerts.CruiseSpeedCalculator;

@SharedPref(SharedPref.Scope.UNIQUE)
interface CalibrationStorage {

  @DefaultInt(0)
  int getActiveAircraft();

  @DefaultInt(0)
  int getActivePropProfile();

  @DefaultFloat(0.01375f)
  float propSpeedFactor1();

  @DefaultFloat(0.01375f)
  float propSpeedFactor2();

  @DefaultFloat(0.01375f)
  float propSpeedFactor3();

  @DefaultFloat(0.01375f)
  float propSpeedFactor4();

  // Circunference = 1.497m
  // Front gear = 34
  // Rear gear = 19
  // Hub gear ratio = 133%
  // Overall gear ratio = 1.33*34/19
  // Speed = freq(Hz) * circunference * gear ratio m/s
  //       = freq(rpm)/60 * circunference * gear ratio m/s
  //       = freq(rpm)/60 * circunference * gear ratio * 3.6 km/h
  @DefaultFloat(1.497f / 60.0f * 3.6f * 1.33f * 34.0f / 19.0f)
  float crankSpeedRatio();

  @DefaultInt(0)
  int getActivePilotProfile();

  @DefaultFloat(CruiseSpeedCalculator.LIGHT_PILOT_WEIGHT_KG)
  float pilot1Weight();

  @DefaultFloat(CruiseSpeedCalculator.MEDIUM_PILOT_WEIGHT_KG)
  float pilot2Weight();

  @DefaultFloat(CruiseSpeedCalculator.HEAVY_PILOT_WEIGHT_KG)
  float pilot3Weight();
}