package org.dash.avionics.sensors;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.dash.avionics.R;
import org.dash.avionics.data.Measurement;
import org.dash.avionics.data.MeasurementStorageColumns;
import org.dash.avionics.display.PFDActivity_;
import org.dash.avionics.sensors.ant.AntSensorManager;
import org.dash.avionics.sensors.arduino.ArduinoSensorManager;
import org.dash.avionics.sensors.attitude.AttitudeSensorManager;
import org.dash.avionics.sensors.fake.FakeSensorManager;
import org.dash.avionics.sensors.gps.GpsSensorManager;
import org.dash.avionics.sensors.network.UDPMeasurementSender;
import org.dash.avionics.sensors.network.UDPSensorManager;
import org.dash.avionics.sensors.viiiiva.ViiiivaSensorManager;
import org.dash.avionics.sensors.weathermeter.KingpostSensorManager;
import org.dash.avionics.sensors.weathermeter.WeatherMeterSensorManager;

@SuppressLint("Registered")
@EService
public class SensorsService extends Service implements SensorListener {
  private static final int REQUEST_STOP = -2;
  private static final String EXTRA_STOP = "STOP";

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
  @Bean
  protected AttitudeSensorManager attitudeSensor;
  @Bean
  protected WeatherMeterSensorManager weatherMeterSensor;
  @Bean
  protected UDPSensorManager udpSensor;
  @Bean
  protected UDPMeasurementSender udpSender;
  @Bean
  protected KingpostSensorManager weatherMeterSensorKingPost;

  private ContentResolver contentResolver;

  @SystemService PowerManager powerManager;
  private PowerManager.WakeLock wakeLock;

  private ImmutableMultiset<SensorManager> startedSensors;

  @Override
  public void onCreate() {
    Log.i("Sensors", "Starting");

    startForeground();

    contentResolver = getContentResolver();

    startSensors();
  }

  private void startForeground() {
    wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sensors");
    wakeLock.acquire();

    Notification.Builder notificationBuilder = new Notification.Builder(this);
    notificationBuilder.setLights(0xffff00ff, 400, 200);
    notificationBuilder.setOngoing(true);
    notificationBuilder.setPriority(Notification.PRIORITY_HIGH);
    notificationBuilder.setContentTitle("DaSH");
    notificationBuilder.setContentText("Recording in the background");
    notificationBuilder.setCategory(Notification.CATEGORY_SERVICE);

    Drawable largeIcon = getResources().getDrawable(R.drawable.ic_launcher, null);
    notificationBuilder.setLargeIcon(((BitmapDrawable) largeIcon).getBitmap());
    notificationBuilder.setSmallIcon(R.drawable.ic_launcher);

    notificationBuilder.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0,
        PFDActivity_.intent(getApplicationContext()).get(), 0));

    Intent stopIntent = new Intent();
    stopIntent.setClass(getApplicationContext(), SensorsService_.class);
    stopIntent.putExtra(EXTRA_STOP, true);

    notificationBuilder.addAction(android.R.drawable.ic_delete, "Stop",
        PendingIntent.getService(this, REQUEST_STOP, stopIntent, 0));
    startForeground(1234, notificationBuilder.build());
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.i("Sensors", "Got start command: id=" + startId + "; intent=" +
        (intent != null ? intent : "null"));
    int result = super.onStartCommand(intent, flags, startId);
    if (intent == null) return result;

    if (intent.getBooleanExtra(EXTRA_STOP, false)) {
      deferStop();
      return result;
    }

    updateStartedSensors();

    return result;
  }

  @Background
  protected void deferStop() {
    stopSelf();
  }

  @Override
  public void onDestroy() {
    Log.i("Sensors", "Stopping");
    stopSensors();

    wakeLock.release();
  }

  private void startSensors() {
    ImmutableMultiset<SensorManager> sensors = getEnabledSensorManagers();
    for (SensorManager sensor : sensors) {
      sensor.connect(this);
    }

    // The preferences may change - we want to be sure we later stop the same managers we just
    // started.
    startedSensors = sensors;
  }

  private void stopSensors() {
    for (SensorManager sensor : startedSensors) {
      sensor.disconnect();
    }
  }

  private void updateStartedSensors() {
    ImmutableMultiset<SensorManager> updatedSensors = getEnabledSensorManagers();
    if (!updatedSensors.equals(startedSensors)) {
      Multiset<SensorManager> newSensors = Multisets.difference(updatedSensors, startedSensors);
      Multiset<SensorManager> oldSensors = Multisets.difference(startedSensors, updatedSensors);
      Log.i("Sensors", "Sensor set changed, restarting: old=" + oldSensors + "; new=" + newSensors);
      for (SensorManager oldSensor : oldSensors) {
        oldSensor.disconnect();
      }
      for (SensorManager newSensor : newSensors) {
        newSensor.connect(this);
      }
      startedSensors = updatedSensors;
    }
  }

  private ImmutableMultiset<SensorManager> getEnabledSensorManagers() {
    ImmutableMultiset.Builder<SensorManager> builder = new ImmutableMultiset.Builder<>();
    if (preferences.isFakeDataEnabled().get()) builder.add(fakeSensor);
    if (preferences.isViiiivaEnabled().get()) builder.add(vivaSensor);
    if (preferences.isWeatherMeterEnabled().get()) builder.add(weatherMeterSensor);
    if (preferences.isKingpostMeterEnabled().get()) builder.add(weatherMeterSensorKingPost);
    if (preferences.isAntPlusEnabled().get()) builder.add(antSensor);
    if (preferences.isArduinoEnabled().get()) builder.add(arduinoSensor);
    if (preferences.isGpsEnabled().get()) builder.add(gpsSensor);
    if (preferences.isAttitudeEnabled().get()) builder.add(attitudeSensor);
    if (preferences.isUdpReceivingEnabled().get()) builder.add(udpSensor);
    return builder.build();
  }

  @Override
  public void onNewMeasurement(Measurement update) {
    udpSender.onNewMeasurement(update);

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
