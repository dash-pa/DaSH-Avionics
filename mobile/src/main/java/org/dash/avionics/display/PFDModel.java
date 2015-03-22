package org.dash.avionics.display;

import com.google.common.collect.Sets;

import org.dash.avionics.data.Measurement;
import org.dash.avionics.data.MeasurementListener;
import org.dash.avionics.display.altitude.AltitudeTape;
import org.dash.avionics.display.climbrate.ClimbRateTape;
import org.dash.avionics.display.model.RecentSettableValueModel;
import org.dash.avionics.display.model.SettableValueModel;
import org.dash.avionics.display.model.ValueModel;
import org.dash.avionics.display.speed.SpeedTape;
import org.schmivits.airball.airdata.Aircraft;

import java.util.Set;

/**
 * Model which receives updates from the data feed and feeds those into the UI models.
 */
public class PFDModel implements SpeedTape.Model, AltitudeTape.Model, ClimbRateTape.Model,
    MeasurementListener {
  private static final long DEFAULT_MAX_DATA_AGE_MS = 2 * 1000;
  // ANT+ needs larger delays
  private static final long ANTPLUS_MAX_DATA_AGE_MS = 5 * 1000;

  private final SettableValueModel<Float> climbRateModel =
      new RecentSettableValueModel<>(DEFAULT_MAX_DATA_AGE_MS);
  private final SettableValueModel<Float> speedModel =
      new RecentSettableValueModel<>(DEFAULT_MAX_DATA_AGE_MS);
  private final SettableValueModel<Float> altitudeModel =
      new RecentSettableValueModel<>(DEFAULT_MAX_DATA_AGE_MS);
  private final SettableValueModel<Aircraft> aircraftModel =
      new RecentSettableValueModel<>(DEFAULT_MAX_DATA_AGE_MS);

  private final Set<Runnable> updateListeners = Sets.newConcurrentHashSet();

  @Override
  public ValueModel<Float> getClimbRate() {
    return climbRateModel;
  }

  @Override
  public ValueModel<Float> getSpeed() {
    return speedModel;
  }

  @Override
  public ValueModel<Aircraft> getAircraft() {
    return aircraftModel;
  }

  @Override
  public ValueModel<Float> getAltitude() {
    return altitudeModel;
  }

  @Override
  public void onNewMeasurement(Measurement measurement) {
    switch (measurement.type) {
      case SPEED:
        speedModel.setValue(measurement.value);
        break;
      case HEIGHT:
        altitudeModel.setValue(measurement.value);
        break;
    }

    // TODO
    aircraftModel.setValue(new Aircraft() {
      @Override
      public float getVs0() {
        return 10;
      }

      @Override
      public float getVs1() {
        return 20;
      }

      @Override
      public float getVfe() {
        return 30;
      }

      @Override
      public float getVno() {
        return 40;
      }

      @Override
      public float getVne() {
        return 50;
      }

      @Override
      public float getAs() {
        return 0.2f;
      }

      @Override
      public float getAmin() {
        return 0.1f;
      }

      @Override
      public float getAx() {
        return 0.3f;
      }

      @Override
      public float getAy() {
        return 0.4f;
      }

      @Override
      public float getAref() {
        return 0.5f;
      }

      @Override
      public float getBfs() {
        return 0.5f;
      }
    });

//    Log.v("PFDModel", "Updated");

    for (Runnable listener : updateListeners) {
      listener.run();
    }
  }

  public void addUpdateListener(Runnable callback) {
    updateListeners.add(callback);
  }

  public void removeUpdateListener(Runnable callback) {
    updateListeners.remove(callback);
  }
}
