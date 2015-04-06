package org.dash.avionics.display;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;

import com.google.common.collect.Sets;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.dash.avionics.aircraft.AircraftSettings_;
import org.dash.avionics.aircraft.CruiseSpeedCalculator;
import org.dash.avionics.data.Measurement;
import org.dash.avionics.data.MeasurementListener;
import org.dash.avionics.data.MeasurementObserver;
import org.dash.avionics.data.model.DerivativeValueModel;
import org.dash.avionics.data.model.MissionAircraftModel;
import org.dash.avionics.data.model.RecentSettableValueModel;
import org.dash.avionics.data.model.SettableValueModel;
import org.dash.avionics.data.model.ValueModel;
import org.dash.avionics.display.altitude.AltitudeTape;
import org.dash.avionics.display.climbrate.ClimbRateTape;
import org.dash.avionics.display.speed.SpeedTape;
import org.dash.avionics.data.model.AircraftModel;

import java.util.Set;

/**
 * Model which receives updates from the data feed and feeds those into the UI models.
 */
@EBean
public class PFDModel implements SpeedTape.Model, AltitudeTape.Model, ClimbRateTape.Model,
    MeasurementListener, SharedPreferences.OnSharedPreferenceChangeListener {
  private static final long DEFAULT_MAX_DATA_AGE_MS = 2 * 1000;
  // ANT+ needs larger delays
  private static final long ANTPLUS_MAX_DATA_AGE_MS = 5 * 1000;

  private final DerivativeValueModel climbRateModel =
      new DerivativeValueModel(DEFAULT_MAX_DATA_AGE_MS);
  private final SettableValueModel<Float> speedModel =
      new RecentSettableValueModel<>(DEFAULT_MAX_DATA_AGE_MS);
  private final SettableValueModel<Float> altitudeModel =
      new RecentSettableValueModel<>(DEFAULT_MAX_DATA_AGE_MS);
  private final SettableValueModel<AircraftModel> aircraftModel = new SettableValueModel<>();

  private final Set<Runnable> updateListeners = Sets.newConcurrentHashSet();
  @RootContext Context context;
  @Pref AircraftSettings_ settings;
  private MeasurementObserver observer;

  public void start() {
    PreferenceManager.getDefaultSharedPreferences(context)
        .registerOnSharedPreferenceChangeListener(this);
    updateAircraftModel();
    this.observer = new MeasurementObserver(new Handler(), context.getContentResolver(), this);
    observer.start();
  }

  public void stop() {
    PreferenceManager.getDefaultSharedPreferences(context)
        .unregisterOnSharedPreferenceChangeListener(this);
    observer.stop();
  }

  @Override
  public ValueModel<Float> getClimbRate() {
    return climbRateModel;
  }

  @Override
  public ValueModel<Float> getSpeed() {
    return speedModel;
  }

  @Override
  public ValueModel<AircraftModel> getAircraft() {
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
        climbRateModel.addValue(measurement.value);
        break;
      default:
        return;
    }

    notifyUpdateListeners();
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    updateAircraftModel();
  }

  private void updateAircraftModel() {
    float targetSpeed = CruiseSpeedCalculator.getCruiseAirspeedFromSettings(settings);
    float speedMargin = settings.getMaxSpeedDelta().get();
    float targetHeight = settings.getTargetHeight().get();
    float heightMargin = settings.getMaxHeightDelta().get();
    aircraftModel.setValue(new MissionAircraftModel(targetSpeed, speedMargin,
        targetHeight, heightMargin));
    notifyUpdateListeners();
  }

  public void addUpdateListener(Runnable callback) {
    updateListeners.add(callback);
  }

  public void removeUpdateListener(Runnable callback) {
    updateListeners.remove(callback);
  }

  private synchronized void notifyUpdateListeners() {
    for (Runnable listener : updateListeners) {
      listener.run();
    }
  }

}
