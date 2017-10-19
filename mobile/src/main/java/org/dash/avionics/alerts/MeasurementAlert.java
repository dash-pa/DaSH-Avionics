package org.dash.avionics.alerts;

import android.support.annotation.Nullable;

import org.dash.avionics.aircraft.AircraftSettings_;
import org.dash.avionics.data.MeasurementType;

public interface MeasurementAlert {

  void updateSettings(AircraftSettings_ settings);

  //Allows for the MeasurementAlert to update any internal measurment trackers and return an AlertType
  //aka MeasurementAlert Status: low, normal, high, unknown
  @Nullable
  AlertType updateMeasurment(MeasurementType type, Float value);
}
