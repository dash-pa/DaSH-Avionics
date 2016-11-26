package org.dash.avionics.aircraft;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.dash.avionics.R;
import org.dash.avionics.data.DataDeleter;
import org.dash.avionics.data.files.CsvDataDumper;
import org.dash.avionics.sensors.SensorPreferences_;

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

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.aircraft_settings);

    bindPreferenceSummary(R.string.settings_key_pilot_weight, settings.getPilotWeight().get());
    bindPreferenceSummary(R.string.settings_key_aircraft_type, settings.getAircraftType().get());
    bindPreferenceSummary(R.string.settings_key_crank_prop_ratio,
        settings.getCrankToPropellerRatio().get());
    bindPreferenceSummary(R.string.settings_key_speed_delta, settings.getMaxSpeedDelta().get());
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
    return findPreference(getActivity().getString(resId));
  }

  @Override
  public boolean onPreferenceChange(Preference preference, Object value) {
    String stringValue = value.toString();

    if (preference instanceof ListPreference) {
      // For list preferences, look up the correct display value in
      // the preference's 'entries' list.
      ListPreference listPreference = (ListPreference) preference;
      int index = listPreference.findIndexOfValue(stringValue);

      // Set the summary to reflect the new value.
      preference.setSummary(
          index >= 0
              ? listPreference.getEntries()[index]
              : null);
    } else {
      // For all other preferences, set the summary to the value's
      // simple string representation.
      preference.setSummary(stringValue);
    }

    updateDerivedValues();

    return true;
  }

  @Override
  public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
    if (getString(R.string.settings_action_dump_data).equals(preference.getKey())) {
      dumper.dumpAllData();
    } else if (getString(R.string.settings_action_erase_data).equals(preference.getKey())) {
      deleter.deleteAllData();
    } else {
      return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
    return true;
  }

  private void updateDerivedValues() {
    float cruiseAirspeed = CruiseSpeedCalculator.getCruiseAirspeedFromSettings(settings);

    findPreference(R.string.settings_key_cruise_speed).setSummary(
        String.format("%.1f", cruiseAirspeed));
  }
}
