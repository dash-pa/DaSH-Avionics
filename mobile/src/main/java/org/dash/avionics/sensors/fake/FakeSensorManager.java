package org.dash.avionics.sensors.fake;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.api.BackgroundExecutor;
import org.dash.avionics.data.Measurement;
import org.dash.avionics.data.MeasurementType;
import org.dash.avionics.sensors.SensorListener;
import org.dash.avionics.sensors.SensorManager;

import java.util.Random;

@EBean
public class FakeSensorManager implements SensorManager {

  private static final MeasurementType[] TYPES_TO_GENERATE = {
      MeasurementType.HEADING};
  public static final int FAKE_DATA_DELAY = 100;
  public static final int MAX_VALUE = 100;

  private final Random random = new Random();
  private SensorListener updater;

  @Override
  public void connect(SensorListener updater) {
    this.updater = updater;

    generateFakeData();
  }

  @Override
  public void disconnect() {
    BackgroundExecutor.cancelAll("data", true);
    this.updater = null;
  }

  @SuppressWarnings("InfiniteRecursion")
  @Background(id = "data", delay = FAKE_DATA_DELAY)
  public void generateFakeData() {
    MeasurementType type = TYPES_TO_GENERATE[random.nextInt(TYPES_TO_GENERATE.length)];

    long now = System.currentTimeMillis();
    switch (type) {
      case GPS_LATITUDE:
      case GPS_LONGITUDE:
      case GPS_ALTITUDE:
      case GPS_SPEED:
        // GPS measurements need to be generated together.
        generateFakeMeasurement(MeasurementType.GPS_LATITUDE, now);
        generateFakeMeasurement(MeasurementType.GPS_LONGITUDE, now);
        generateFakeMeasurement(MeasurementType.GPS_ALTITUDE, now);
        generateFakeMeasurement(MeasurementType.GPS_SPEED, now);
        generateFakeMeasurement(MeasurementType.GPS_BEARING, now);
        break;
      default:
        generateFakeMeasurement(type, now);
        break;
    }

    // Go again after the delay.
    generateFakeData();
  }

  private void generateFakeMeasurement(MeasurementType type, long when) {
    float value = random.nextFloat() * MAX_VALUE;
    Measurement measurement = new Measurement(type, value, when);
    if (updater != null) {
      updater.onNewMeasurement(measurement);
    }
  }
}
