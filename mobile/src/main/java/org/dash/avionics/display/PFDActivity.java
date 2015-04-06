package org.dash.avionics.display;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Fullscreen;
import org.androidannotations.annotations.LongClick;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.dash.avionics.R;
import org.dash.avionics.aircraft.AircraftSettingsActivity_;
import org.dash.avionics.sensors.SensorsService_;

/**
 * Primary Flight Display.
 */
@EActivity(R.layout.activity_pfd)
@Fullscreen
public class PFDActivity extends Activity {
  @ViewById
  protected PFDView pfdView;

  private Intent serviceIntent;
  @Bean PFDModel model;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    serviceIntent = SensorsService_.intent(getApplicationContext()).get();
    startService(serviceIntent);
  }

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
  }

  @Override
  protected void onPause() {
    model.stop();

    super.onPause();
  }

  @UiThread(delay = 100)
  protected void hideSystemUi() {
    //noinspection ConstantConditions
    getActionBar().hide();
  }

  @Override
  protected void onDestroy() {
    stopService(serviceIntent);

    super.onDestroy();
  }

  @LongClick(R.id.pfdView)
  protected void onLongClickI() {
    AircraftSettingsActivity_.intent(this).start();
  }
}
