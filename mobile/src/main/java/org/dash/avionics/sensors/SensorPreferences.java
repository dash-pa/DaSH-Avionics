package org.dash.avionics.sensors;

import org.androidannotations.annotations.sharedpreferences.DefaultBoolean;
import org.androidannotations.annotations.sharedpreferences.SharedPref;
import org.dash.avionics.R;

@SharedPref(SharedPref.Scope.APPLICATION_DEFAULT)
public interface SensorPreferences {

  @DefaultBoolean(value=false, keyRes = R.string.settings_key_sensor_arduino)
  boolean isArduinoEnabled();

  @DefaultBoolean(value=false, keyRes = R.string.settings_key_sensor_viiiiva)
  boolean isViiiivaEnabled();

  @DefaultBoolean(value=false, keyRes = R.string.settings_key_sensor_antplus)
  boolean isAntPlusEnabled();

  @DefaultBoolean(value=false, keyRes = R.string.settings_key_sensor_gps)
  boolean isGpsEnabled();

  @DefaultBoolean(value=true, keyRes = R.string.settings_key_sensor_fake)
  boolean isFakeDataEnabled();

}