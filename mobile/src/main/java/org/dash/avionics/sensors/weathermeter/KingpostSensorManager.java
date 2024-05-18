package org.dash.avionics.sensors.weathermeter;

import android.annotation.SuppressLint;
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
    String kingpostUUID = preferences.getKingpostWmUUID().get();
    @SuppressLint("MissingPermission") String deviceName = device.getName();

    return (deviceName != null && (deviceName.contains("787") || deviceName.contains("782")))
            || device.getAddress().compareToIgnoreCase(kingpostUUID) == 0;
  }

  protected MeasurementType getMeasurmentType() {
    return MeasurementType.KINGPOST_SPEED;
  }

}
