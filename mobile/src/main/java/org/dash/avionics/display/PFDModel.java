package org.dash.avionics.display;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.dash.avionics.aircraft.AircraftSettings_;
import org.dash.avionics.aircraft.CruiseSpeedCalculator;
import org.dash.avionics.data.Measurement;
import org.dash.avionics.data.MeasurementListener;
import org.dash.avionics.data.MeasurementObserver;
import org.dash.avionics.data.model.AircraftModel;
import org.dash.avionics.data.model.DerivativeValueModel;
import org.dash.avionics.data.model.MissionAircraftModel;
import org.dash.avionics.data.model.RecentSettableValueModel;
import org.dash.avionics.data.model.SettableValueModel;
import org.dash.avionics.data.model.ValueModel;
import org.dash.avionics.display.altitude.AltitudeTape;
import org.dash.avionics.display.climbrate.ClimbRateTape;
import org.dash.avionics.display.crank.CrankGauge;
import org.dash.avionics.display.prop.PropGauge;
import org.dash.avionics.display.speed.SpeedTape;
import org.dash.avionics.display.track.TrackDrawing;
import org.dash.avionics.display.vitals.VitalsDisplay;

import java.util.List;
import java.util.Set;

/**
 * Model which receives updates from the data feed and feeds those into the UI models.
 */
@EBean
public class PFDModel implements SpeedTape.Model, AltitudeTape.Model, ClimbRateTape.Model,
    MeasurementListener, SharedPreferences.OnSharedPreferenceChangeListener, CrankGauge.Model,
    PropGauge.Model, VitalsDisplay.Model, TrackDrawing.Model {
  private static final long DEFAULT_MAX_DATA_AGE_MS = 2 * 1000;
  // ANT+ needs larger delays
  private static final long ANTPLUS_MAX_DATA_AGE_MS = 5 * 1000;
  private static final long LOCATION_HISTORY_AGE_MS = 10 * 60 * 1000;

  private final DerivativeValueModel climbRateModel =
      new DerivativeValueModel(DEFAULT_MAX_DATA_AGE_MS);
  private final SettableValueModel<Float> speedModel =
      new RecentSettableValueModel<>(DEFAULT_MAX_DATA_AGE_MS);
  private final SettableValueModel<Float> altitudeModel =
      new RecentSettableValueModel<>(DEFAULT_MAX_DATA_AGE_MS);
  private final RecentSettableValueModel<Float> heading =
      new RecentSettableValueModel<>(DEFAULT_MAX_DATA_AGE_MS);
  private final RecentSettableValueModel<Float> crankRpm =
      new RecentSettableValueModel<>(ANTPLUS_MAX_DATA_AGE_MS);
  private final RecentSettableValueModel<Float> crankPower =
      new RecentSettableValueModel<>(ANTPLUS_MAX_DATA_AGE_MS);
  private final RecentSettableValueModel<Float> propRpm =
      new RecentSettableValueModel<>(ANTPLUS_MAX_DATA_AGE_MS);
  private final RecentSettableValueModel<Float> heartRate =
      new RecentSettableValueModel<>(ANTPLUS_MAX_DATA_AGE_MS);
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
  public ValueModel<Float> getHeading() {
    return heading;
  }

  @Override
  public ValueModel<Float> getCrankRpm() {
    return crankRpm;
  }

  @Override
  public ValueModel<Float> getCrankPower() {
    return crankPower;
  }

  @Override
  public ValueModel<Float> getPropRpm() {
    return propRpm;
  }

  @Override
  public ValueModel<Float> getHeartRate() {
    return heartRate;
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
      case CRANK_RPM:
        crankRpm.setValue(measurement.value);
        // TODO: Make ratio configurable
        propRpm.setValue(measurement.value / 60f * 34f);
        break;
      case POWER:
        crankPower.setValue(measurement.value);
        break;
      case HEART_BEAT:
        heartRate.setValue(measurement.value);
        break;
      case GPS_ALTITUDE:
      case GPS_LATITUDE:
      case GPS_LONGITUDE:
      case GPS_SPEED:
      case GPS_BEARING:
        updateGpsPosition(measurement);
        break;
      case HEADING:
        heading.setValue(measurement.value);
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

  private Long lastGpsTimestamp;
  private Float lastGpsLatitude;
  private Float lastGpsLongitude;
  private Float lastGpsAltitude;
  private Float lastGpsSpeed;
  private Float lastGpsBearing;
  private List<Location> locationHistory = Lists.newLinkedList();

  private void updateGpsPosition(Measurement measurement) {
    if (lastGpsTimestamp != null && measurement.timestamp != lastGpsTimestamp) {
      lastGpsTimestamp = null;
      lastGpsLatitude = null;
      lastGpsLongitude = null;
      lastGpsAltitude = null;
      lastGpsSpeed = null;
      lastGpsBearing = null;
    }

    lastGpsTimestamp = measurement.timestamp;

    switch (measurement.type) {
      case GPS_LATITUDE:
        lastGpsLatitude = measurement.value;
        break;
      case GPS_LONGITUDE:
        lastGpsLongitude = measurement.value;
        break;
      case GPS_ALTITUDE:
        lastGpsAltitude = measurement.value;
        break;
      case GPS_SPEED:
        lastGpsSpeed = measurement.value;
        break;
      case GPS_BEARING:
        lastGpsBearing = measurement.value;
        break;
      default:
        Log.w("PFDModel", "Unexpected GPS type: " + measurement.type);
        return;
    }

    if (lastGpsLatitude != null && lastGpsLongitude != null && lastGpsAltitude != null &&
        lastGpsSpeed != null && lastGpsBearing != null) {
      Location loc = new Location("GPS");
      loc.setTime(measurement.timestamp);
      loc.setLatitude(lastGpsLatitude);
      loc.setLongitude(lastGpsLongitude);
      loc.setAltitude(lastGpsAltitude);
      loc.setSpeed(lastGpsSpeed);
      loc.setBearing(lastGpsBearing);

      addLocationToHistory(loc);
    }
  }

  private void addLocationToHistory(Location loc) {
    long now = System.currentTimeMillis();
    synchronized (locationHistory) {
      locationHistory.add(loc);

      // Clean up old locations.
      while (!locationHistory.isEmpty() &&
          now - locationHistory.get(0).getTime() > LOCATION_HISTORY_AGE_MS) {
        locationHistory.remove(0);
      }
    }
  }

  @Override
  public List<Location> getLocationHistory() {
    synchronized (locationHistory) {
      return Lists.newArrayList(locationHistory);
    }
  }
}
