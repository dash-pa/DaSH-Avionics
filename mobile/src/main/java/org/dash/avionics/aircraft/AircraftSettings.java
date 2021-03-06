package org.dash.avionics.aircraft;

import org.androidannotations.annotations.sharedpreferences.DefaultFloat;
import org.androidannotations.annotations.sharedpreferences.DefaultString;
import org.androidannotations.annotations.sharedpreferences.SharedPref;
import org.dash.avionics.R;

@SharedPref(SharedPref.Scope.APPLICATION_DEFAULT)
public interface AircraftSettings {
  @DefaultString(value = "V5", keyRes = R.string.settings_key_aircraft_type)
  String getAircraftType();

  @DefaultFloat(value = 75.0f, keyRes = R.string.settings_key_pilot_weight)
  float getPilotWeight();

  @DefaultFloat(value = 2.0f, keyRes = R.string.settings_key_speed_delta)
  float getMaxSpeedDelta();

  @DefaultFloat(value = 5.0f, keyRes = R.string.settings_key_target_height)
  float getTargetHeight();

  @DefaultFloat(value = 21.6f, keyRes = R.string.settings_key_rotate_speed)
  float getRotateAirspeed();

  @DefaultFloat(value = 2.0f, keyRes = R.string.settings_key_height_delta)
  float getMaxHeightDelta();

  @DefaultFloat(value = 34f, keyRes = R.string.settings_key_crank_prop_ratio)
  float getCrankToPropellerRatio();

}
