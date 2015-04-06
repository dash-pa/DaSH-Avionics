package org.dash.avionics.alerts;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.dash.avionics.aircraft.AircraftSettings_;
import org.dash.avionics.aircraft.CruiseSpeedCalculator;
import org.dash.avionics.data.Measurement;
import org.dash.avionics.data.MeasurementListener;
import org.dash.avionics.data.MeasurementObserver;
import org.dash.avionics.data.MeasurementType;

import java.util.List;
import java.util.Map;
import java.util.Set;

@EBean
public class MeasurementAlerter
    implements MeasurementListener, SharedPreferences.OnSharedPreferenceChangeListener {
  // Only start "unknown" sound alerts after 2s.
  private static final long MAX_DATA_STALENESS_MS = 2000;

  private static class AlertTypeMapping {
    private AlertTypeMapping(AlertType low, AlertType normal, AlertType high, AlertType unknown) {
      this.low = low;
      this.normal = normal;
      this.high = high;
      this.unknown = unknown;
    }

    AlertType low, normal, high, unknown;
  }
  private static final Map<MeasurementType, AlertTypeMapping> ALERT_MAPPING =
      ImmutableMap.<MeasurementType, AlertTypeMapping>builder()
          .put(MeasurementType.SPEED, new AlertTypeMapping(AlertType.LOW_SPEED,
              AlertType.NORMAL_SPEED, AlertType.HIGH_SPEED, AlertType.UNKNOWN_SPEED))
          .put(MeasurementType.HEIGHT, new AlertTypeMapping(AlertType.LOW_HEIGHT,
              AlertType.NORMAL_HEIGHT, AlertType.HIGH_HEIGHT, AlertType.UNKNOWN_HEIGHT))
          .build();

  private final List<AlertListener> listeners = Lists.newArrayList();
  private final Map<MeasurementType, Range<Float>> expectedRanges =
      Maps.newEnumMap(MeasurementType.class);
  private Map<MeasurementType, Long> lastUpdateByType =
      Maps.newEnumMap(MeasurementType.class);
  private final Set<MeasurementType> hadNormalReadings = Sets.newHashSet();
  private final Set<AlertType> activeAlerts = Sets.newHashSet();

  @RootContext
  Context context;

  @Pref
  AircraftSettings_ settings;

  private MeasurementObserver observer;

  // Notifications are sent only once.
  public interface AlertListener {
    void onAlertsChanged(Set<AlertType> types);
  }

  public void registerListener(AlertListener listener) {
    synchronized (listeners) {
      listeners.add(listener);
    }
  }

  public void unregisterListener(AlertListener listener) {
    synchronized (listeners) {
      listeners.remove(listener);
    }
  }

  public void start() {
    activeAlerts.clear();

    // Assume no initial staleness.
    long now = System.currentTimeMillis();
    for (MeasurementType type : MeasurementType.values()) {
      lastUpdateByType.put(type, now);
    }

    PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
    updateSettings();
    this.observer = new MeasurementObserver(new Handler(), context.getContentResolver(), this);
    observer.start();
    periodicAlertUpdate();
  }

  public void stop() {
    PreferenceManager.getDefaultSharedPreferences(context)
        .unregisterOnSharedPreferenceChangeListener(this);
    observer.stop();
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    updateSettings();
  }

  private void updateSettings() {
    synchronized (expectedRanges) {
      float targetSpeed = CruiseSpeedCalculator.getCruiseAirspeedFromSettings(settings);
      float speedMargin = settings.getMaxSpeedDelta().get();
      expectedRanges.put(MeasurementType.SPEED,
          Range.closed(targetSpeed - speedMargin, targetSpeed + speedMargin));
    }
  }

  @Override
  public synchronized void onNewMeasurement(Measurement measurement) {
    synchronized (lastUpdateByType) {
      lastUpdateByType.put(measurement.type, System.currentTimeMillis());
    }
    updateAlert(measurement.type, measurement.value);
  }

  @Background(id = "alert_watchdog", delay = 500)
  void periodicAlertUpdate() {
    long now = System.currentTimeMillis();
    synchronized (lastUpdateByType) {
      for (Map.Entry<MeasurementType, Long> entry : lastUpdateByType.entrySet()) {
        if (now - entry.getValue() > MAX_DATA_STALENESS_MS) {
          updateAlert(entry.getKey(), null);
        }
      }
    }

    periodicAlertUpdate();
  }

  private void updateAlert(MeasurementType type, Float value) {
    Range<Float> expectedRange;
    synchronized (expectedRanges) {
      expectedRange = expectedRanges.get(type);
    }
    if (expectedRange == null) {
      return;
    }
    AlertTypeMapping mapping = ALERT_MAPPING.get(type);

    if (!hadNormalReadings.contains(type)) {
      // Don't alert about unknown or abnormal values until the first time a normal value is seen.
      // This accounts for edge conditions such as takeoff (low speed, low altitude) which then
      // transition into the normal regime (cruise).
      if (value == null || !expectedRange.contains(value)) {
        return;
      }

      hadNormalReadings.add(type);
    }

    synchronized (activeAlerts) {
      Set<AlertType> newActiveAlerts = Sets.newHashSet(activeAlerts);
      if (value == null) {
        newActiveAlerts.remove(mapping.low);
        newActiveAlerts.remove(mapping.normal);
        newActiveAlerts.remove(mapping.high);
        newActiveAlerts.add(mapping.unknown);
      } else if (expectedRange.contains(value)) {
        newActiveAlerts.remove(mapping.low);
        newActiveAlerts.add(mapping.normal);
        newActiveAlerts.remove(mapping.high);
        newActiveAlerts.remove(mapping.unknown);
      } else if (value <= expectedRange.lowerEndpoint()) {
        newActiveAlerts.add(mapping.low);
        newActiveAlerts.remove(mapping.normal);
        newActiveAlerts.remove(mapping.high);
        newActiveAlerts.remove(mapping.unknown);
      } else {
        Preconditions.checkState(value >= expectedRange.upperEndpoint());
        newActiveAlerts.remove(mapping.low);
        newActiveAlerts.remove(mapping.normal);
        newActiveAlerts.add(mapping.high);
        newActiveAlerts.remove(mapping.unknown);
      }

      Sets.SetView<AlertType> alertChanges =
          Sets.symmetricDifference(activeAlerts, newActiveAlerts);
      if (!alertChanges.isEmpty()) {
        activeAlerts.clear();
        activeAlerts.addAll(newActiveAlerts);

        synchronized (listeners) {
          for (AlertListener listener : listeners) {
            listener.onAlertsChanged(activeAlerts);
          }
        }
      }
    }
  }
}
