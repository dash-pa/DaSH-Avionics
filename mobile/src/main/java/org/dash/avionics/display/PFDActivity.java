package org.dash.avionics.display;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.dash.avionics.R;
import org.dash.avionics.data.MeasurementObserver;
import org.dash.avionics.display.util.SystemUiHider;
import org.dash.avionics.sensors.SensorsService_;

/**
 * An example full-screen activity that shows and hides the system UI (i.e. status bar and
 * navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
@EActivity(R.layout.activity_pfd)
public class PFDActivity extends Activity {
  @ViewById
  protected PFDView pfdView;

  private Intent serviceIntent;
  private PFDModel model;
  private MeasurementObserver observer;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    serviceIntent = SensorsService_.intent(getApplicationContext()).get();
    startService(serviceIntent);

    model = new PFDModel();
    observer = new MeasurementObserver(new Handler(), getContentResolver(), model);
    observer.start();
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

    observer.start();
  }

  @Override
  protected void onPause() {
    observer.stop();

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
}
