package org.dash.avionics.sensors.attitude;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.SystemService;
import org.dash.avionics.data.Measurement;
import org.dash.avionics.data.MeasurementType;
import org.dash.avionics.sensors.SensorListener;

/**
 * Sensor manager which reads attitude information from Android's internal sensors and outputs it.
 */
@EBean
public class AttitudeSensorManager implements org.dash.avionics.sensors.SensorManager, SensorEventListener {

  @SystemService
  SensorManager sensors;

  private SensorListener updater;

  @Override
  public void connect(SensorListener updater) {
    this.updater = updater;

    Sensor rotationSensor = sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    if (!sensors.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)) {
      Log.e("AttitudeSensor", "Failed to register heading listener");
    }
  }

  @Override
  public void disconnect() {
    sensors.unregisterListener(this);
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    if (event.sensor.getType() != Sensor.TYPE_ROTATION_VECTOR) {
      return;
    }

    float[] orientation = new float[3];
    float[] rotationMatrix = new float[9];
    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
    SensorManager.getOrientation(rotationMatrix, orientation);
    float azimuth = (float) Math.toDegrees(orientation[0]);
    while (azimuth > 360.0f) azimuth -= 360.0f;
    while (azimuth < 0.0f) azimuth += 360.0f;

    updater.onNewMeasurement(new Measurement(MeasurementType.HEADING, azimuth,
        event.timestamp / 1000000L));
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
    Log.i("AttitudeSensor", "Accuracy for sensor " + sensor.getStringType() + " is " + accuracy);
  }
}
