package org.dash.avionics;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import org.dash.avionics.calibration.CalibrationManager;
import org.dash.avionics.calibration.CalibrationProfile;
import org.dash.avionics.calibration.RatioTracker;
import org.dash.avionics.data.Measurement;
import org.dash.avionics.data.MeasurementListener;
import org.dash.avionics.data.MeasurementObserver;
import org.dash.avionics.data.MeasurementType;
import org.dash.avionics.sensors.SensorsService_;

import java.util.Locale;

@Fullscreen
@EActivity(R.layout.activity_calibration)
@SuppressLint("Registered")
public class ImpellerCalibrationActivity extends Activity implements
    MeasurementListener {

  @ViewById
  TextView knownSpeedView, impellerRpmView, calculatedSpeedView,
      immediateRatioView, averageRatioView, usedRatioView;

  private MeasurementObserver observer;

  @Bean
  CalibrationManager calibrationManager;

  private CalibrationProfile calibration;

  private RatioTracker impellerRatio;

  @Click
  protected void useImmediateButton() {
    calibration.setImpellerRatio(impellerRatio.getLastRatio());
    updateRatios();
  }

  @Click
  protected void useAverageButton() {
    calibration.setImpellerRatio(impellerRatio.getMaxTimeAverage());
    updateRatios();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent = SensorsService_.intent(getApplicationContext()).get();
    startForegroundService(intent);

    observer = new MeasurementObserver(new Handler(), getContentResolver(),
        this);
    impellerRatio = new RatioTracker(MeasurementType.IMPELLER_SPEED, MeasurementType.IMPELLER_RPM);

    //noinspection ConstantConditions
    getActionBar().hide();
  }

  @Override
  protected void onResume() {
    super.onResume();

    observer.start();
    updateRatiosBackground();
  }

  @Override
  protected void onPause() {
    BackgroundExecutor.cancelAll("watchdog", true);
    observer.stop();

    super.onPause();
  }

  @AfterViews
  protected void loadProfile() {
    changeProfile(0);
  }

  @Override
  public void onNewMeasurement(Measurement measurement) {
    TextView viewToUpdate;
    switch (measurement.type) {
      case IMPELLER_RPM:
        viewToUpdate = impellerRpmView;
        impellerRatio.addDenominator(measurement);
        break;
      case CRANK_RPM:
        viewToUpdate = knownSpeedView;

        // Transform crank RPM into speed
        measurement = new Measurement(MeasurementType.IMPELLER_SPEED, measurement.value *
            calibration.getCrankSpeedRatio(), measurement.timestamp);

        impellerRatio.addNumerator(measurement);
        break;
      default:
        return;
    }

    setValue(measurement.value, viewToUpdate);

    updateRatios();
  }

  private void setValue(float value, TextView view) {
    String valueStr = String.format(Locale.US, "%.3f", value);
    view.setText(valueStr);
  }

  // TODO: Also add a watchdog like AvionicsActivity (move to service?)
  @SuppressWarnings("InfiniteRecursion")
  @Background(id = "ratioUpdater", delay = 200)
  protected void updateRatiosBackground() {
    updateRatios();
    updateRatiosBackground();
  }

  @UiThread
  protected void updateRatios() {
    float ratio = calibration.getImpellerRatio();
    setValue(ratio, usedRatioView);
    setValue(impellerRatio.getLastDenominator() * ratio, calculatedSpeedView);
    setValue(impellerRatio.getLastRatio(), immediateRatioView);
    setValue(impellerRatio.getMaxTimeAverage(), averageRatioView);
  }

  private int getProfileIndex(View button) {
    int id = button.getId();
    switch (id) {
      case R.id.profile1Button:
        return 0;
      case R.id.profile2Button:
        return 1;
      case R.id.profile3Button:
        return 2;
      case R.id.profile4Button:
        return 3;
      default:
        throw new IllegalArgumentException("Bad view with ID " + id);
    }
  }

  @Click({R.id.profile1Button, R.id.profile2Button, R.id.profile3Button,
      R.id.profile4Button})
  protected void changeProfile(View clicked) {
    int profileIdx = getProfileIndex(clicked);
    changeProfile(profileIdx);
  }

  private void changeProfile(int profileIdx) {
    calibration = calibrationManager.loadImpellerProfile(profileIdx);
    usedRatioView.setText(Float.toString(calibration.getImpellerRatio()));
  }
}
