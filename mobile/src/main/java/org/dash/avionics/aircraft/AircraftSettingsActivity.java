package org.dash.avionics.aircraft;

import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import androidx.core.app.NavUtils;
import android.view.MenuItem;

import org.androidannotations.annotations.EActivity;
import org.dash.avionics.BTActivityPermissionManager;

import java.util.List;

@EActivity
public class AircraftSettingsActivity extends PreferenceActivity {

  private BTActivityPermissionManager btpm = new BTActivityPermissionManager();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (!btpm.checkPermissions()) {
      btpm.requestPermissions(this);
    } else {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    btpm.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == android.R.id.home) {
      // This ID represents the Home or Up button. In the case of this
      // activity, the Up button is shown. Use NavUtils to allow users
      // to navigate up one level in the application structure. For
      // more details, see the Navigation pattern on Android Design:
      //
      // http://developer.android.com/design/patterns/navigation.html#up-vs-back
      //
      // TODO: If Settings has multiple levels, Up should navigate up
      // that hierarchy.
      NavUtils.navigateUpFromSameTask(this);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onBuildHeaders(List<Header> target) {
    super.onBuildHeaders(target);

    getFragmentManager().beginTransaction()
        .replace(android.R.id.content, new AircraftSettingsFragment_())
        .commit();
  }

  @Override
  protected boolean isValidFragment(String fragmentName) {
    return AircraftSettingsFragment_.class.getName().equals(fragmentName);
  }
}
