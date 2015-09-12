package org.dash.avionics.sensors.gps;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.SystemService;
import org.dash.avionics.data.Measurement;
import org.dash.avionics.data.MeasurementType;
import org.dash.avionics.sensors.SensorListener;
import org.dash.avionics.sensors.SensorManager;

@EBean
public class GpsSensorManager implements SensorManager, LocationListener {
  private static final long GPS_MIN_TIME_MS = 500L;
  private static final float GPS_MIN_DISTANCE_M = .1f;

  @SystemService
  LocationManager locationManager;

  private SensorListener updater;

  @Override
  public void connect(SensorListener updater) {
    this.updater = updater;
    Log.i("GpsSensor", "Connecting");
    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_MIN_TIME_MS,
        GPS_MIN_DISTANCE_M, this);
  }

  @Override
  public void disconnect() {
    Log.i("GpsSensor", "Disconnecting");
    locationManager.removeUpdates(this);
  }

  @Override
  public void onLocationChanged(Location location) {
//    Log.d("GpsSensors", "Loc: " + location);
    long when = location.getTime();
    float latitude = (float) location.getLatitude();
    float longitude = (float) location.getLongitude();
    float altitude = (float) location.getAltitude();
    float speed = (float) location.getSpeed();
    float bearing = (float) location.getBearing();
    while (bearing > 360.0f) bearing -= 360.0f;
    while (bearing < 0.0f) bearing += 360.0f;

    updater.onNewMeasurement(
        new Measurement(MeasurementType.GPS_LATITUDE, latitude, when));
    updater.onNewMeasurement(
        new Measurement(MeasurementType.GPS_LONGITUDE, longitude, when));
    updater.onNewMeasurement(
        new Measurement(MeasurementType.GPS_ALTITUDE, altitude, when));
    updater.onNewMeasurement(
        new Measurement(MeasurementType.GPS_SPEED, speed, when));
    updater.onNewMeasurement(
        new Measurement(MeasurementType.GPS_BEARING, bearing, when));
  }

  @Override
  public void onStatusChanged(String provider, int status, Bundle extras) {
    Log.i("GpsSensor", "Provider status changed: provider=" + provider + "; status=" + status +
        "; extras=" + extras);
  }

  @Override
  public void onProviderEnabled(String provider) {
    Log.i("GpsSensor", "Provider enabled: " + provider);
  }

  @Override
  public void onProviderDisabled(String provider) {
    Log.i("GpsSensor", "Provider disabled: " + provider);

  }
}
