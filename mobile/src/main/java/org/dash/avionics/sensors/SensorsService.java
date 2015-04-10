package org.dash.avionics.sensors;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.SystemService;
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
  private static final boolean USE_FAKE_DATA = true;
  private static final boolean USE_VIIIIVA = false;
  private static final boolean USE_ARDUINO = false;
  private static final boolean USE_ANT = false;
  private static final boolean USE_GPS = true;

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
    contentResolver = getContentResolver();

    wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sensors");
    wakeLock.acquire();

    if (USE_FAKE_DATA) fakeSensor.connect(this);
    if (USE_VIIIIVA) vivaSensor.connect(this);
    if (USE_ANT) antSensor.connect(this);
    if (USE_ARDUINO) arduinoSensor.connect(this);
    if (USE_GPS) gpsSensor.connect(this);
  }

  @Override
  public void onDestroy() {
    if (USE_FAKE_DATA) fakeSensor.disconnect();
    if (USE_VIIIIVA) vivaSensor.disconnect();
    if (USE_ANT) antSensor.disconnect();
    if (USE_ARDUINO) arduinoSensor.disconnect();
    if (USE_GPS) gpsSensor.disconnect();

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
