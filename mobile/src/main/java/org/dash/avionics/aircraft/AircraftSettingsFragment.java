package org.dash.avionics.aircraft;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.Toast;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.dash.avionics.R;
import org.dash.avionics.data.DataDeleter;
import org.dash.avionics.data.files.CsvDataDumper;
import org.dash.avionics.sensors.SensorPreferences_;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

@EFragment
public class AircraftSettingsFragment extends PreferenceFragment
    implements Preference.OnPreferenceChangeListener {
  private static final String PREFERENCE_NAME = "Aircraft";

  @Pref
  AircraftSettings_ settings;

  @Pref
  SensorPreferences_ sensorPreferences;

  @Bean
  CsvDataDumper dumper;

  @Bean
  DataDeleter deleter;

  private AircraftType currentAircraftType;
  private float currentPilotWeight;

  private static final int DUMP_ALL_DATA_TO_CSV = 2;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.aircraft_settings);

    currentPilotWeight = settings.getPilotWeight().get();
    currentAircraftType = AircraftType.valueOf(settings.getAircraftType().get());
    bindPreferenceSummary(R.string.settings_key_pilot_weight, currentPilotWeight);
    bindPreferenceSummary(R.string.settings_key_aircraft_type, currentAircraftType);
    bindPreferenceSummary(R.string.settings_key_crank_prop_ratio,
        settings.getCrankToPropellerRatio().get());
    bindPreferenceSummary(R.string.settings_key_speed_delta, settings.getMaxSpeedDelta().get());
    bindPreferenceSummary(R.string.settings_key_rotate_speed, settings.getRotateAirspeed().get());
    bindPreferenceSummary(R.string.settings_key_target_height, settings.getTargetHeight().get());
    bindPreferenceSummary(R.string.settings_key_height_delta, settings.getMaxHeightDelta().get());
    bindPreferenceSummary(R.string.settings_key_send_udp_address,
        sensorPreferences.getUdpSendingAddress().get());

    updateIpAddresses();
  }

  private void updateIpAddresses() {
    StringBuffer summaryBuffer = new StringBuffer();
    try {
      List<NetworkInterface> interfaces = Collections.list(NetworkInterface
          .getNetworkInterfaces());
      for (NetworkInterface iface : interfaces) {
        List<InetAddress> addresses = Collections.list(iface.getInetAddresses());
        for (InetAddress addr : addresses) {
          if (!addr.isLoopbackAddress() &&
              !addr.isLinkLocalAddress() &&
              !addr.isMulticastAddress()) {
            Log.d("IFACE", "Addr: " + addr.getHostAddress());
            if (summaryBuffer.length() > 0) {
              summaryBuffer.append('\n');
            }
            summaryBuffer.append(addr.getHostAddress());
          }
        }
      }
    } catch (SocketException e) {
      Log.w("Preferences", "Unable to get IP addresses", e);
      return;
    }

    Preference myIp = findPreference(R.string.settings_key_my_ip);
    myIp.setSummary(summaryBuffer.toString());
  }

  private void bindPreferenceSummary(int resId, Object currentValue) {
    Preference preference = findPreference(resId);
    preference.setOnPreferenceChangeListener(this);
    onPreferenceChange(preference, currentValue);
  }

  private Preference findPreference(int resId) {
    return findPreference(getPreferenceKeyByResId(resId));
  }

  private CharSequence getPreferenceKeyByResId(int resId) {
    return getActivity().getString(resId);
  }

  @Override
  public boolean onPreferenceChange(Preference preference, Object value) {
    String stringValue = value.toString();

    if (preference instanceof ListPreference) {
      // For list preferences, look up the correct display value in
      // the preference's 'entries' list.
      ListPreference listPreference = (ListPreference) preference;
      int index = listPreference.findIndexOfValue(stringValue);
      CharSequence enumValue = listPreference.getEntries()[index];
      // Set the summary to reflect the new value.
      preference.setSummary(index >= 0 ? enumValue : null);
      if (preference.getKey() == getPreferenceKeyByResId(R.string.settings_key_aircraft_type)) {
        currentAircraftType = AircraftType.valueOf(stringValue);
      }
    } else {
      // For all other preferences, set the summary to the value's
      // simple string representation.
      preference.setSummary(stringValue);
      if (preference.getKey() == getPreferenceKeyByResId(R.string.settings_key_pilot_weight)) {
        currentPilotWeight = Float.valueOf(stringValue);
      }
    }

    updateDerivedValues();

    return true;
  }

  @Override
  public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
    if (getString(R.string.settings_action_dump_data).equals(preference.getKey())) {
      // Ask the user where to save the file
      String fileName = dumper.formatTimestamp(System.currentTimeMillis()) + ".csv";
      Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      intent.setType("application/csv");
      intent.putExtra(Intent.EXTRA_TITLE, fileName);
      intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOCUMENTS);
      startActivityForResult(intent, DUMP_ALL_DATA_TO_CSV);
    } else if (getString(R.string.settings_action_erase_data).equals(preference.getKey())) {
      deleter.deleteAllData();
    } else {
      return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
    return true;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
    if ((requestCode == DUMP_ALL_DATA_TO_CSV) && (resultCode == Activity.RESULT_OK)) {
      // The result data contains a URI for the document or directory that
      // the user selected.
      Uri uri = null;
      if (resultData != null) {
        uri = resultData.getData();
        ParcelFileDescriptor pfd = null;
        try {
          pfd = getActivity().getContentResolver().openFileDescriptor(uri, "w");
        } catch (IOException e) {
          Log.w("DUMP", "Failed to write CSV", e);
          Toast.makeText(getContext(), R.string.dump_failed, Toast.LENGTH_LONG).show();
          return;
        }
        FileOutputStream output = new FileOutputStream(pfd.getFileDescriptor());
        // Dump the data to the requested file
        dumper.dumpAllData(output);
      }
    }
  }

//  @Override
//  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//    switch (requestCode) {
//      case FILE_PERM_REQ_CODE:
//        // If request is cancelled, the result arrays are empty.
//        if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
//          btEnabled = true;
//        }  else {
//          btEnabled = false;
//        }
//        break;
//      case LOC_REQ_CODE:
//        // If request is cancelled, the result arrays are empty.
//        if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
//          locEnabled = true;
//        }  else {
//          locEnabled = false;
//        }
//        break;
//    }
//    if (btEnabled && locEnabled && (serviceIntent == null)) {
//      Log.i("PFDActivity", "Starting sensors after permissions granted");
//      serviceIntent = SensorsService_.intent(getApplicationContext()).get();
//      startForegroundService(serviceIntent);
//    }
//  }

  private void updateDerivedValues() {
    float cruiseAirspeed = CruiseSpeedCalculator.getCruiseAirspeed(currentAircraftType, currentPilotWeight);

    findPreference(R.string.settings_key_cruise_speed).setSummary(
        String.format("%.1f", cruiseAirspeed));
  }
}
