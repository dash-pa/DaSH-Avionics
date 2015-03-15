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

  private static final MeasurementType TYPES_TO_GENERATE[] = {
      MeasurementType.PROP_RPM, MeasurementType.CRANK_RPM};

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

  @Background(id = "data", delay = 250)
  public void generateFakeData() {
    MeasurementType type = TYPES_TO_GENERATE[random.nextInt(TYPES_TO_GENERATE.length)];
    float value = random.nextFloat() * 100f;
    Measurement measurement = new Measurement(type, value);
    updater.onNewMeasurement(measurement);

    // Go again after the delay.
    generateFakeData();
  }
}