package org.dash.avionics.aircraft;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.dash.avionics.data.Measurement;
import org.dash.avionics.data.MeasurementListener;
import org.dash.avionics.data.MeasurementObserver;
import org.dash.avionics.data.MeasurementType;

@EBean
public class CruiseSpeedAlerter
    implements MeasurementListener, SharedPreferences.OnSharedPreferenceChangeListener {
  @RootContext
  Context context;
  @Pref
  AircraftSettings_ settings;

  private MeasurementObserver observer;
  private CruiseSpeedAlertListener listener;
  private float targetSpeed;
  private float speedMargin;
  private boolean alerting = false;

  // Notifications are sent only once.
  public interface CruiseSpeedAlertListener {
    void onLowSpeed();

    void onHighSpeed();

    void onStoppedAlerting();
  }

  public CruiseSpeedAlerter() {
    this.observer = new MeasurementObserver(new Handler(), context.getContentResolver(), this);
  }

  public void start(CruiseSpeedAlertListener listener) {
    this.listener = listener;

    PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
    updateSettings();
    observer.start();
  }

  public void stop() {
    PreferenceManager.getDefaultSharedPreferences(context)
        .unregisterOnSharedPreferenceChangeListener(this);
    observer.stop();
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    updateSettings();
  }

  private void updateSettings() {
    targetSpeed = CruiseSpeedCalculator.getCruiseAirspeedFromSettings(settings);
    speedMargin = settings.getMaxSpeedDelta().get();
  }

  @Override
  public synchronized void onNewMeasurement(Measurement measurement) {
    if (measurement.type != MeasurementType.SPEED) {
      return;
    }

    float speed = measurement.value;
    boolean alreadyAlerting = alerting;
    alerting = true;
    if (speed > targetSpeed + speedMargin) {
      if (!alreadyAlerting) listener.onHighSpeed();
    } else if (speed < targetSpeed - speedMargin) {
      if (!alreadyAlerting) listener.onLowSpeed();
    } else {
      alerting = false;
      if (alreadyAlerting) listener.onStoppedAlerting();
    }
  }
}
