package org.dash.avionics.sensors;

import org.dash.avionics.data.Measurement;

import java.util.Map;

public interface SensorListener {
  void onNewMeasurement(Measurement measurement);

  default void onDeviceListChange(Map<String, String> devices) { }

}