package org.dash.avionics;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.util.Log;

public class BTActivityPermissionManager {
  private final int BT_REQ_CODE = 1;
  private final int LOC_REQ_CODE = 2;

  private boolean locEnabled = false;

  private boolean btEnabled = false;

  public boolean requestPermissions(Activity activity) {
    if (activity != null) {
      Log.d("BTActivity", "checkPermissions");
      // Before we start the sensors, ask for permissions
      if (activity.checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") != PackageManager.PERMISSION_GRANTED) {
        Log.w("BTActivity", "Requesting location permissions");
        activity.requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION"}, LOC_REQ_CODE);
      } else {
        locEnabled = true;
      }
      if (locEnabled &&
              ((activity.checkSelfPermission("android.permission.BLUETOOTH_SCAN") != PackageManager.PERMISSION_GRANTED) ||
                      (activity.checkSelfPermission("android.permission.BLUETOOTH_CONNECT") != PackageManager.PERMISSION_GRANTED))
      ) {
        Log.w("BTActivity", "Requesting Bluetooth permissions");
        activity.requestPermissions(
                new String[]{"android.permission.BLUETOOTH_SCAN", "android.permission.BLUETOOTH_CONNECT"},
                BT_REQ_CODE
        );
      } else {
        btEnabled = true;
      }
    }

    return btEnabled && locEnabled;
  }

  public boolean checkPermissions() {
    return btEnabled && locEnabled;
  }

  public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    switch (requestCode) {
      case BT_REQ_CODE:
        // If request is cancelled, the result arrays are empty.
        if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
          btEnabled = true;
        }  else {
          btEnabled = false;
        }
        break;
      case LOC_REQ_CODE:
        // If request is cancelled, the result arrays are empty.
        if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
          locEnabled = true;
        }  else {
          locEnabled = false;
        }
        break;
    }

    return checkPermissions();
  }
}
