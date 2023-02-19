package org.dash.avionics.display;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

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
  @Bean
  PFDModel model;
  @Bean
  MeasurementAlerter speedAlerter;
  @Bean
  MeasurementAlertSounds speedAlertSounds;

  private final int BT_REQ_CODE = 1;
  private final int LOC_REQ_CODE = 2;

  private boolean locEnabled = false;

  private boolean btEnabled = false;

  private boolean requestingPermission = false;

  @AfterViews
  protected void setModel() {
    pfdView.setModel(model);
  }

  private void checkPermissions() {
    // Before we start the sensors, ask for permissions
    if (checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") != PackageManager.PERMISSION_GRANTED) {
      if (!requestingPermission) {
        requestingPermission = true;
        Log.w("PFDActivity", "Requesting location permissions");
        requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION"}, LOC_REQ_CODE);
      }
    } else {
      locEnabled = true;
    }
    if (
        (checkSelfPermission("android.permission.BLUETOOTH_SCAN") != PackageManager.PERMISSION_GRANTED) ||
            (checkSelfPermission("android.permission.BLUETOOTH_CONNECT") != PackageManager.PERMISSION_GRANTED)
    ) {
      if (!requestingPermission) {
        requestingPermission = true;
        Log.w("PFDActivity", "Requesting Bluetooth permissions");
        requestPermissions(
            new String[]{"android.permission.BLUETOOTH_SCAN", "android.permission.BLUETOOTH_CONNECT"},
            BT_REQ_CODE
        );
      }
    } else {
      btEnabled = true;
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    switch (requestCode) {
      case BT_REQ_CODE:
        requestingPermission = false;
        // If request is cancelled, the result arrays are empty.
        if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
          btEnabled = true;
        } else {
          btEnabled = false;
        }
        break;
      case LOC_REQ_CODE:
        requestingPermission = false;
        // If request is cancelled, the result arrays are empty.
        if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
          locEnabled = true;
        } else {
          locEnabled = false;
        }
        break;
    }
    if (btEnabled && locEnabled && (serviceIntent == null)) {
      Log.i("PFDActivity", "Starting sensors after permissions granted");
      serviceIntent = SensorsService_.intent(getApplicationContext()).get();
      startForegroundService(serviceIntent);
    }
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    checkPermissions();

    // Trigger the initial hide() shortly after the activity has been
    // created, to briefly hint to the user that UI controls
    // are available.
    hideSystemUi();
  }

  @Override
  protected void onResume() {
    super.onResume();
    checkPermissions();

    model.start();
    speedAlertSounds.start();
    speedAlerter.registerListener(speedAlertSounds);
    speedAlerter.start();

    serviceIntent = SensorsService_.intent(getApplicationContext()).get();
    startForegroundService(serviceIntent);
  }

  @Override
  protected void onPause() {
//    stopService(serviceIntent);

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
//    Disabling this as it prevents the first click on the UI from registering which creates user confusion
//    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
  }

  @Click(R.id.settings_button)
  protected void onSettingsButtonClicked() {
    AircraftSettingsActivity_.intent(this).start();
  }

  @LongClick(R.id.pfdView)
  protected void onLongClickI() {
    // Disabling long click as this occurs during phone mounting often.
    // Leaving the function registered to prevent a long click from being read
    // as a regular click.
    // AircraftSettingsActivity_.intent(this).start();
  }

  @Click(R.id.pfdView)
  protected void onClickUI() {
    pfdView.onPFDClicked();
  }
}
