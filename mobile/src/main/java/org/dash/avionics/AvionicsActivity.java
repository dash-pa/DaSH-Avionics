package org.dash.avionics;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Fullscreen;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.api.BackgroundExecutor;
import org.dash.avionics.alerts.CruiseSpeedAlerter;
import org.dash.avionics.alerts.CruiseSpeedAlerter.CruiseSpeedAlertListener;
import org.dash.avionics.calibration.CalibrationManager;
import org.dash.avionics.calibration.CalibrationProfile;
import org.dash.avionics.data.Measurement;
import org.dash.avionics.data.MeasurementListener;
import org.dash.avionics.data.MeasurementObserver;
import org.dash.avionics.data.MeasurementType;
import org.dash.avionics.sensors.SensorsService_;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Fullscreen
@EActivity(R.layout.activity_avionics)
@SuppressLint("Registered")
public class AvionicsActivity extends Activity
    implements MeasurementListener, CruiseSpeedAlertListener {
  private static final long DEFAULT_MAX_DATA_AGE_MS = 2 * 1000;
  // ANT+ needs larger delays
  private static final long ANTPLUS_MAX_DATA_AGE_MS = 5 * 1000;
  private static final Map<MeasurementType, Long> MAX_DATA_AGES_MS = new HashMap<>();

  static {
    MAX_DATA_AGES_MS.put(MeasurementType.CRANK_RPM, DEFAULT_MAX_DATA_AGE_MS);
    MAX_DATA_AGES_MS.put(MeasurementType.HEADING, DEFAULT_MAX_DATA_AGE_MS);
    MAX_DATA_AGES_MS.put(MeasurementType.HEIGHT, DEFAULT_MAX_DATA_AGE_MS);
    MAX_DATA_AGES_MS.put(MeasurementType.SPEED, DEFAULT_MAX_DATA_AGE_MS);

    MAX_DATA_AGES_MS.put(MeasurementType.POWER, ANTPLUS_MAX_DATA_AGE_MS);
    MAX_DATA_AGES_MS.put(MeasurementType.HEART_BEAT, ANTPLUS_MAX_DATA_AGE_MS);
  }

  @ViewById
  protected TextView rpmView, powerView, heartView, headingView, speedView, heightView;

  private Map<MeasurementType, TextView> viewsByType = new HashMap<>(10);
  private Map<MeasurementType, Long> lastUpdateByType = new ConcurrentHashMap<>(10);

  private MeasurementObserver observer;

  @Bean
  protected CalibrationManager calibrationManager;
  private CalibrationProfile calibration;
  private CruiseSpeedAlerter speedAlerter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent = SensorsService_.intent(getApplicationContext()).get();
    startService(intent);

    observer = new MeasurementObserver(new Handler(), getContentResolver(),
        this);
    speedAlerter = new CruiseSpeedAlerter(this);

    //noinspection ConstantConditions
    getActionBar().hide();
  }

  @AfterViews
  protected void setValues() {
    viewsByType.put(MeasurementType.CRANK_RPM, rpmView);
    viewsByType.put(MeasurementType.POWER, powerView);
    viewsByType.put(MeasurementType.HEART_BEAT, heartView);
    viewsByType.put(MeasurementType.HEADING, headingView);
    viewsByType.put(MeasurementType.SPEED, speedView);
    viewsByType.put(MeasurementType.HEIGHT, heightView);
  }

  @Override
  protected void onResume() {
    super.onResume();

    // Assumes that the profile doesn't change without going somewhere else to change it.
    calibration = calibrationManager.loadActiveProfile();
    speedAlerter.setCalibration(calibration);

    observer.start();
    runWatchdog();
  }

  @Override
  protected void onPause() {
    BackgroundExecutor.cancelAll("watchdog", true);
    observer.stop();

    super.onPause();
  }

  @SuppressWarnings("InfiniteRecursion")
  @Background(id = "watchdog", delay = 500)
  protected void runWatchdog() {
    long now = System.currentTimeMillis();
    for (MeasurementType type : MeasurementType.values()) {
      Long maxAge = MAX_DATA_AGES_MS.get(type);
      if (maxAge == null) {
        continue;
      }

      Long lastTimestamp = lastUpdateByType.get(type);
      if (lastTimestamp == null || lastTimestamp < now - maxAge) {
        if (lastTimestamp != null) {
          Log.w("Watchdog", "No update for type " + type + " for " + (now - lastTimestamp) + "ms");
        }
        setValueUnknown(type);
      }
    }

    runWatchdog();
  }

  @UiThread
  protected void setValueUnknown(MeasurementType type) {
    TextView view = viewsByType.get(type);
    if (view == null) {
      return;
    }

    view.setText(R.string.value_not_available);
  }

  @UiThread
  protected void setValue(Measurement update) {
    TextView view = viewsByType.get(update.type);
    if (view == null) {
      return;
    }

    String valueStr = String.format(Locale.US, "%.1f", update.value);
    view.setText(valueStr);
  }

  @Override
  public void onNewMeasurement(Measurement update) {
    lastUpdateByType.put(update.type, System.currentTimeMillis());
    setValue(update);

    updateAlerters(update);
    updateDerivedValues(update);
  }

  private void updateDerivedValues(Measurement update) {
    if (update.type == MeasurementType.PROP_RPM) {
      float speed = update.value * calibration.getPropRatio();
      onNewMeasurement(new Measurement(MeasurementType.SPEED, speed,
          update.timestamp));
    }
  }

  private void updateAlerters(Measurement update) {
    if (update.type == MeasurementType.SPEED) {
      speedAlerter.updateCurrentSpeed(update.value);
    }
  }

  @Override
  public void onLowSpeed() {
    speedView.setTextColor(getResources().getColor(R.color.alert));
  }

  @Override
  public void onHighSpeed() {
    speedView.setTextColor(getResources().getColor(R.color.alert));
  }

  @Override
  public void onStoppedAlerting() {
    speedView.setTextColor(getResources().getColor(android.R.color.primary_text_dark));
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    View decorView = getWindow().getDecorView();
    if (hasFocus) {
      // TODO: Support older versions
      decorView
          .setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
      // | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
  }

  @Click(R.id.speedView)
  public void onSpeedClicked() {
    PropCalibrationActivity_.intent(this).start();
  }
}
