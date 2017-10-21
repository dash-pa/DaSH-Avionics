package org.dash.avionics.alerts;

import android.support.annotation.Nullable;

import org.dash.avionics.aircraft.AircraftSettings_;
import org.dash.avionics.data.MeasurementType;

import java.util.Set;

public interface MeasurementAlert {

  void updateSettings(AircraftSettings_ settings);

  //Allows for the MeasurementAlert to update any internal measurment trackers and update the set of active alerts
  void updateMeasurment(MeasurementType type, Float value, Set<AlertType> activeAlerts);
}
