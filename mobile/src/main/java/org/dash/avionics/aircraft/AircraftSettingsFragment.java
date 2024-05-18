package org.dash.avionics.aircraft;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
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
import org.dash.avionics.data.Measurement;
import org.dash.avionics.data.files.CsvDataDumper;
import org.dash.avionics.sensors.SensorListener;
import org.dash.avionics.sensors.SensorPreferences_;
import org.dash.avionics.sensors.btle.BTLESensorPrefScanner;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
  private String currentAirspeedSensor;

  private BTLESensorPrefScanner wmScanner;

  private static final int DUMP_ALL_DATA_TO_CSV = 2;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.aircraft_settings);

    currentPilotWeight = settings.getPilotWeight().get();
    currentAircraftType = AircraftType.valueOf(settings.getAircraftType().get());
    bindOnPreferenceChange(R.string.settings_key_pilot_weight, currentPilotWeight);
    bindOnPreferenceChange(R.string.settings_key_aircraft_type, currentAircraftType);
    bindOnPreferenceChange(
      R.string.settings_key_crank_prop_ratio,
      settings.getCrankToPropellerRatio().get()
    );
    bindOnPreferenceChange(R.string.settings_key_speed_delta, settings.getMaxSpeedDelta().get());
    bindOnPreferenceChange(R.string.settings_key_rotate_speed, settings.getRotateAirspeed().get());
    bindOnPreferenceChange(R.string.settings_key_target_height, settings.getTargetHeight().get());
    bindOnPreferenceChange(R.string.settings_key_height_delta, settings.getMaxHeightDelta().get());
    bindOnPreferenceChange(
      R.string.settings_key_send_udp_address,
      sensorPreferences.getUdpSendingAddress().get()
    );

    bindWmSensorLists();
    setupWsSensScan();
    updateIpAddresses();
  }

  private void bindWmSensorLists() {
    bindOnPreferenceChange(
            R.string.settings_key_sensor_weathermeter_select,
            sensorPreferences.getWeathermeterUUID().get()
    );
    bindOnPreferenceChange(
            R.string.settings_key_sensor_kingpost_select,
            sensorPreferences.getKingpostWmUUID().get()
    );
    // Ensure the previously selected sensors show in the list for weathermeters
    String wmn = sensorPreferences.getWeathermeterName().get();
    String wmu = sensorPreferences.getWeathermeterUUID().get();
    String kpn = sensorPreferences.getKingpostWmName().get();
    String kpu = sensorPreferences.getKingpostWmUUID().get();
    if (wmu != "" && wmu != null) {
      ListPreference wml = (ListPreference) findPreference(R.string.settings_key_sensor_weathermeter_select);
      wml.setEntryValues(new CharSequence[]{wmu, kpu});
      wml.setEntries(new CharSequence[]{wmn, kpn});
      wml.setSummary(wmn);
    }
    if (kpu != "" && kpu != null) {
      ListPreference kpl = (ListPreference) findPreference(R.string.settings_key_sensor_kingpost_select);
      kpl.setEntryValues(new CharSequence[]{wmu, kpu});
      kpl.setEntries(new CharSequence[]{wmn, kpn});
      kpl.setSummary(kpn);
    }
  }

  private void setupWsSensScan() {
    wmScanner = new BTLESensorPrefScanner(
            getContext(),
            Arrays.asList("WFAN"),
            Arrays.asList(
                    // Adding to list by characteristic not yet supported
//                      UUID.fromString("961f0005-d2d6-43e3-a417-3bb8217e0e01")
            )
    ) {
      @SuppressLint("MissingPermission")
      @Override
      public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Log.i("PSCAN", gatt.getDevice().getName() + ":" + gatt.getDevice().getAddress());
      }
    };

    Preference wmsp = findPreference(R.string.settings_key_sensor_weathermeter_select);
    if (!(wmsp instanceof ListPreference)) {
      return;
    }
    ListPreference wml = (ListPreference) wmsp;
    Preference kpsp = findPreference(R.string.settings_key_sensor_kingpost_select);
    if (!(kpsp instanceof ListPreference)) {
      return;
    }
    ListPreference kpl = (ListPreference) kpsp;
    wmScanner.startScan(new SensorListener() {
      @Override
      public void onNewMeasurement(Measurement measurement) {
        Log.d("WMSCANNER", measurement.toString());
      }
      @Override
      public void onDeviceListChange(Map<String, String> devices) {
        String wmn = sensorPreferences.getWeathermeterName().get();
        String wmu = sensorPreferences.getWeathermeterUUID().get();
        if (wmu != null && wmu != "" && !devices.containsKey(wmu)) {
          devices.put(wmu, wmn);
        }
        String kpn = sensorPreferences.getKingpostWmName().get();
        String kpu = sensorPreferences.getKingpostWmUUID().get();
        if (kpu != null && kpu != "" && !devices.containsKey(kpu)) {
          devices.put(kpu, kpn);
        }
        Log.d("WMSCANNER", "Devices: " + devices);
        Set<String> entryKeys = devices.keySet();
        Collection<String> entryVals = devices.values();
        Log.d("WMSCANNER", "keys: " + entryKeys);
        Log.d("WMSCANNER", "vals: " + entryVals);
        wml.setEntries(entryVals.toArray(new String[0]));
        wml.setEntryValues(entryKeys.toArray(new String[0]));
        kpl.setEntries(entryVals.toArray(new String[0]));
        kpl.setEntryValues(entryKeys.toArray(new String[0]));
      }
    });
  }

  @Override
  public void onStop() {
    wmScanner.stopScan();
    super.onStop();
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

  private void bindOnPreferenceChange(int resId, Object currentValue) {
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
      ListPreference listPreference = (ListPreference) preference;
      int index = listPreference.findIndexOfValue(stringValue);
      CharSequence enumValue = "";
      if (index >= 0 && index < listPreference.getEntries().length) {
        enumValue = listPreference.getEntries()[index];
      }
      // Set the summary to reflect the new value.
      preference.setSummary(index >= 0 ? enumValue : null);
      if (preference.getKey() == getPreferenceKeyByResId(R.string.settings_key_aircraft_type)) {
        currentAircraftType = AircraftType.valueOf(stringValue);
      } else if (preference.getKey() == getPreferenceKeyByResId(R.string.settings_key_sensor_weathermeter_select)) {
        if (stringValue != sensorPreferences.getWeathermeterUUID().get()) {
          sensorPreferences.getWeathermeterName().put(enumValue.toString());
          Log.i("WMSCANNER", "storing wm name " + enumValue);
        }
      } else if (preference.getKey() == getPreferenceKeyByResId(R.string.settings_key_sensor_kingpost_select)) {
        if (stringValue != sensorPreferences.getKingpostWmUUID().get()) {
          sensorPreferences.getKingpostWmName().put(enumValue.toString());
          Log.i("WMSCANNER", "storing kp name " + enumValue);
        }
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
