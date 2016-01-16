package org.dash.avionics.display;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Fullscreen;
import org.androidannotations.annotations.LongClick;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.dash.avionics.R;
import org.dash.avionics.aircraft.AircraftSettingsActivity_;
import org.dash.avionics.alerts.MeasurementAlertSounds;
import org.dash.avionics.alerts.MeasurementAlerter;
import org.dash.avionics.sensors.SensorsService_;

/**
 * Primary Flight Display.
 */
@EActivity(R.layout.activity_pfd)
@Fullscreen
public class PFDActivity extends Activity {
  @ViewById
  protected PFDView_ pfdView;

  private Intent serviceIntent;
  @Bean PFDModel model;
  @Bean
  MeasurementAlerter speedAlerter;
  @Bean
  MeasurementAlertSounds speedAlertSounds;

  @AfterViews
  protected void setModel() {
    pfdView.setModel(model);
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    // Trigger the initial hide() shortly after the activity has been
    // created, to briefly hint to the user that UI controls
    // are available.
    hideSystemUi();
  }

  @Override
  protected void onResume() {
    super.onResume();

    model.start();
    speedAlertSounds.start();
    speedAlerter.registerListener(speedAlertSounds);
    speedAlerter.start();

    serviceIntent = SensorsService_.intent(getApplicationContext()).get();
    startService(serviceIntent);
  }

  @Override
  protected void onPause() {
    stopService(serviceIntent);

    speedAlerter.unregisterListener(speedAlertSounds);
    speedAlerter.stop();
    speedAlertSounds.stop();
    model.stop();

    super.onPause();
  }

  @UiThread(delay = 100)
  protected void hideSystemUi() {
    //noinspection ConstantConditions
    getActionBar().hide();

    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
  }

  @LongClick(R.id.pfdView)
  protected void onLongClickI() {
    AircraftSettingsActivity_.intent(this).start();
  }

  @Click(R.id.pfdView)
  protected void onClickUI() {
    pfdView.onPFDClicked();
  }
}
