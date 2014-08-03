package org.dash.avionics;

import java.util.Locale;

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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

@Fullscreen
@EActivity(R.layout.activity_calibration)
public class CalibrationActivity extends Activity implements
		MeasurementListener {

	@ViewById
	TextView knownSpeedView, propRpmView, calculatedSpeedView,
			immediateRatioView, averageRatioView, usedRatioView;

	private MeasurementObserver observer;

	@Bean
	CalibrationManager calibrationManager;

	private CalibrationProfile calibration;

	private RatioTracker propRatio;

	@Click
	protected void useImmediateButton() {
		calibration.setPropRatio(propRatio.getLastRatio());
		updateRatios();
	}

	@Click
	protected void useAverageButton() {
		calibration.setPropRatio(propRatio.getMaxTimeAverage());
		updateRatios();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = SensorsService_.intent(getApplicationContext()).get();
		startService(intent);

		observer = new MeasurementObserver(new Handler(), getContentResolver(),
				this);
		propRatio = new RatioTracker(MeasurementType.SPEED, MeasurementType.PROP_RPM);

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
		TextView viewToUpdate = null;
		switch (measurement.type) {
		case PROP_RPM:
			viewToUpdate = propRpmView;
			propRatio.addDenominator(measurement);
			break;
		case CRANK_RPM:
			viewToUpdate = knownSpeedView;

			// Transform crank RPM into speed
			measurement = new Measurement(MeasurementType.SPEED, measurement.value * calibration.getCrankSpeedRatio(), measurement.timestamp);

			propRatio.addNumerator(measurement);
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
	@Background(id = "ratioUpdater", delay = 200)
	protected void updateRatiosBackground() {
		updateRatios();
		updateRatiosBackground();
	}

	@UiThread
	protected void updateRatios() {
		float ratio = calibration.getPropRatio();
		setValue(ratio, usedRatioView);
		setValue(propRatio.getLastDenominator() * ratio, calculatedSpeedView);
		setValue(propRatio.getLastRatio(), immediateRatioView);
		setValue(propRatio.getMaxTimeAverage(), averageRatioView);
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

	@Click({ R.id.profile1Button, R.id.profile2Button, R.id.profile3Button,
			R.id.profile4Button })
	protected void changeProfile(View clicked) {
		int profileIdx = getProfileIndex(clicked);
		changeProfile(profileIdx);
	}

	private void changeProfile(int profileIdx) {
		calibration = calibrationManager.getProfile(profileIdx);
		usedRatioView.setText(Float.toString(calibration.getPropRatio()));
	}
}
