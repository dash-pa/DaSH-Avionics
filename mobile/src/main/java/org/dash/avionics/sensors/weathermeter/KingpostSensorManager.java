package org.dash.avionics.sensors.weathermeter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import org.androidannotations.annotations.EBean;
import org.dash.avionics.data.MeasurementType;

/**
 * Manager for the Kingpost WeatherMeter sensor.
 * Will only pair with a single weathermeter "WFAN-02 787"
 */
@EBean
public class KingpostSensorManager extends WeatherMeterSensorManager {

  protected MeasurementType type = MeasurementType.KINGPOST_SPEED;

  public KingpostSensorManager(Context context) {
    super(context);
  }

  protected boolean onDeviceFound(BluetoothDevice device) {
    //Only Weathermeter Device #787 can be used for the kingpost
    return device.getName().contains("787");
  }

  protected MeasurementType getMeasurmentType() {
    return MeasurementType.KINGPOST_SPEED;
  }

}
