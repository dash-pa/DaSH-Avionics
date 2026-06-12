package org.dash.avionics.sensors.weathermeter;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import org.androidannotations.annotations.EBean;
import org.dash.avionics.data.MeasurementType;

import java.util.Optional;

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
    String kingpostUUID = Optional.ofNullable(preferences.getKingpostNameAndAddress().get())
            .map(s -> s.split("#")[1])
            .orElse("");
    String deviceAddr = Optional.ofNullable(device.getAddress()).orElse("");
    @SuppressLint("MissingPermission") String deviceName =  Optional.ofNullable(device.getName()).orElse("");

    if (deviceAddr.compareToIgnoreCase(kingpostUUID) == 0) {
      preferences.activeKingpostMeterId().put(deviceName + "#" + deviceAddr);

      return true;
    }

    return false;
  }

  protected MeasurementType getMeasurmentType() {
    return MeasurementType.KINGPOST_SPEED;
  }

}
