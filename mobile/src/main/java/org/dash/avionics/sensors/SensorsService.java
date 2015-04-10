package org.dash.avionics.sensors;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.dash.avionics.data.Measurement;
import org.dash.avionics.data.MeasurementStorageColumns;
import org.dash.avionics.sensors.ant.AntSensorManager;
import org.dash.avionics.sensors.arduino.ArduinoSensorManager;
import org.dash.avionics.sensors.fake.FakeSensorManager;
import org.dash.avionics.sensors.gps.GpsSensorManager;
import org.dash.avionics.sensors.viiiiva.ViiiivaSensorManager;

@SuppressLint("Registered")
@EService
public class SensorsService extends Service implements SensorListener {
  @Pref
  SensorPreferences_ preferences;

  /*
   * Managers for many types of sensors.
   */
  @Bean
  protected ArduinoSensorManager arduinoSensor;
  @Bean
  protected AntSensorManager antSensor;
  @Bean
  protected FakeSensorManager fakeSensor;
  @Bean
  protected ViiiivaSensorManager vivaSensor;
  @Bean
  protected GpsSensorManager gpsSensor;

  private ContentResolver contentResolver;

  @SystemService PowerManager powerManager;
  private PowerManager.WakeLock wakeLock;

  @Override
  public void onCreate() {
    Log.i("Sensors", "Starting");
    contentResolver = getContentResolver();

    wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sensors");
    wakeLock.acquire();

    if (preferences.isFakeDataEnabled().get()) fakeSensor.connect(this);
    if (preferences.isViiiivaEnabled().get()) vivaSensor.connect(this);
    if (preferences.isAntPlusEnabled().get()) antSensor.connect(this);
    if (preferences.isArduinoEnabled().get()) arduinoSensor.connect(this);
    if (preferences.isGpsEnabled().get()) gpsSensor.connect(this);
  }

  @Override
  public void onDestroy() {
    Log.i("Sensors", "Stopping");
    if (preferences.isFakeDataEnabled().get()) fakeSensor.disconnect();
    if (preferences.isViiiivaEnabled().get()) vivaSensor.disconnect();
    if (preferences.isAntPlusEnabled().get()) antSensor.disconnect();
    if (preferences.isArduinoEnabled().get()) arduinoSensor.disconnect();
    if (preferences.isGpsEnabled().get()) gpsSensor.disconnect();

    wakeLock.release();
  }

  @Override
  public void onNewMeasurement(Measurement update) {
    ContentValues values = new ContentValues();
    values.put(MeasurementStorageColumns.VALUE_TYPE, update.type.ordinal());
    values.put(MeasurementStorageColumns.VALUE_TIMESTAMP, update.timestamp);
    values.put(MeasurementStorageColumns.VALUE, update.value);
    contentResolver.insert(MeasurementStorageColumns.MEASUREMENTS_URI, values);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
