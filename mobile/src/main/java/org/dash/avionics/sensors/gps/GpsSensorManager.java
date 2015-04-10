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
  private static final long GPS_MIN_TIME_MS = 2000L;
  private static final float GPS_MIN_DISTANCE_M = 1.0f;

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
    long when = location.getTime();
    updater.onNewMeasurement(
        new Measurement(MeasurementType.GPS_LATITUDE, (float) location.getLatitude(), when));
    updater.onNewMeasurement(
        new Measurement(MeasurementType.GPS_LONGITUDE, (float) location.getLongitude(), when));
    updater.onNewMeasurement(
        new Measurement(MeasurementType.GPS_ALTITUDE, (float) location.getAltitude(), when));
    updater.onNewMeasurement(
        new Measurement(MeasurementType.GPS_SPEED, (float) location.getSpeed(), when));
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
