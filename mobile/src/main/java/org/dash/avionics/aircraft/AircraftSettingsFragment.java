package org.dash.avionics.aircraft;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.dash.avionics.R;

@EFragment
public class AircraftSettingsFragment extends PreferenceFragment
    implements Preference.OnPreferenceChangeListener {
  private static final String PREFERENCE_NAME = "Aircraft";

  @Pref
  AircraftSettings_ settings;
  private AircraftType currentAircraftType;
  private float currentPilotWeight;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.aircraft_settings);

    bindPreferenceSummary(R.string.settings_key_pilot_weight, settings.getPilotWeight().get());
    bindPreferenceSummary(R.string.settings_key_aircraft_type, settings.getAircraftType().get());
    bindPreferenceSummary(R.string.settings_key_speed_delta, settings.getMaxSpeedDelta().get());
    bindPreferenceSummary(R.string.settings_key_target_height, settings.getTargetHeight().get());
    bindPreferenceSummary(R.string.settings_key_height_delta, settings.getMaxHeightDelta().get());
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

  private void updateDerivedValues() {
    float cruiseAirspeed = CruiseSpeedCalculator.getCruiseAirspeedFromSettings(settings);

    findPreference(R.string.settings_key_cruise_speed).setSummary(
        String.format("%.1f", cruiseAirspeed));
  }
}
