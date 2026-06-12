package org.dash.avionics.sensors.weathermeter;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.dash.avionics.data.Measurement;
import org.dash.avionics.data.MeasurementType;
import org.dash.avionics.sensors.SensorPreferences_;
import org.dash.avionics.sensors.btle.BTLESensorManager;

import java.util.Optional;
import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT16;

/**
 * Manager for the WeatherMeter sensor.
 */
@EBean
public class WeatherMeterSensorManager extends BTLESensorManager {

  private static final UUID WIND_SERVICE_UUID =
          UUID.fromString("961f0001-d2d6-43e3-a417-3bb8217e0e01");
  private static final UUID ENVIRO_CHARACTERISTIC =
          UUID.fromString("961f0005-d2d6-43e3-a417-3bb8217e0e01");

  public WeatherMeterSensorManager(Context context) {
    super(context, "WFANO");
  }

  @Pref
  SensorPreferences_ preferences;

  @Background
  @Override
  protected void enableCharacteristics() {
    // enable heart rate notifications
    enableCharacteristic(WIND_SERVICE_UUID, ENVIRO_CHARACTERISTIC, "wind");
  }

  @Override
  public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
    if (characteristic.getUuid().equals(ENVIRO_CHARACTERISTIC)) {
      handleWindData(characteristic);
    }
  }

  @SuppressLint("MissingPermission")
  @Override
  protected boolean onDeviceFound(BluetoothDevice device) {
    String primaryUUID = Optional.ofNullable(preferences.getWeathermeterNameAndAddress().get())
            .map(s -> s.contains("#") ? s.split("#")[1] : s)
            .orElse("");
    String kingpostUUID = Optional.ofNullable(preferences.getKingpostNameAndAddress().get())
            .map(s -> s.contains("#") ? s.split("#")[1] : s)
            .orElse("");
    String deviceName = device.getName();
    String deviceAddr = device.getAddress();

    // If this is a windmeter, ensure it is in the preferences list of meters
    if (preferences.isKingpostMeterEnabled().get() && deviceAddr.compareToIgnoreCase(kingpostUUID) == 0) {
      return false;
    }
    // If a meter was selected in prefs, only use that one
    if (deviceAddr.compareToIgnoreCase(primaryUUID) != 0) {
      Log.w("WeatherMeterSensorManager", "Rejecting device " + deviceName + "#" + deviceAddr + " as primary wind speed" );
      return false;
    }

    preferences.activeWeathermeterId().put(deviceName + "#" + deviceAddr);
    Log.i("WeatherMeterSensorManager", "Connecting device " + deviceName + "#" + deviceAddr + " as primary wind speed" );


    return true;
  }


  private void handleWindData(BluetoothGattCharacteristic characteristic) {
    int flag = characteristic.getProperties();
    int rpm = characteristic.getIntValue(FORMAT_UINT16, 0);
    float speed = rpms2kph(rpm);

//    Log.d("WeatherMeter", (getMeasurmentType() == MeasurementType.KINGPOST_SPEED ? "KingPost " : "Airspeed ") +
//      "New wind notification: rpm=" + rpm + "; speed=" + speed +
//      "values=" + Arrays.toString(characteristic.getValue())
//    );

    getListener().onNewMeasurement(new Measurement(getMeasurmentType(), speed));
  }

  /** Convert RPM to km/h. */
  private float rpms2kph(int rpm) {
    if (rpm < 60) return 0.0f;
    // Determined empirically by measuring various known airspeeds then curve fitting.
    return 0.003016216f * rpm + 1.232432f;
  }

  protected MeasurementType getMeasurmentType() {
    return MeasurementType.AIRSPEED;
  }
}
