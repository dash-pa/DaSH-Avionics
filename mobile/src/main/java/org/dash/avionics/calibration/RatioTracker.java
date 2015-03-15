package org.dash.avionics.calibration;

import org.dash.avionics.data.Measurement;
import org.dash.avionics.data.MeasurementType;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class RatioTracker {

  public static final long MAX_AGE = 5000;
  private static final long MAX_CLEANUP_INTERVAL_MS = 1000;

  private final MeasurementType numeratorType, denominatorType;
  private List<Measurement> numerators = new LinkedList<>();
  private List<Measurement> denominators = new LinkedList<>();
  private long lastCleanupTime;

  public RatioTracker(MeasurementType numeratorType,
                      MeasurementType denominatorType) {
    this.numeratorType = numeratorType;
    this.denominatorType = denominatorType;
  }

  public void addNumerator(Measurement m) {
    checkType(m, numeratorType);
    numerators.add(m);
    maybeCleanup();
  }

  public void addDenominator(Measurement m) {
    checkType(m, denominatorType);
    denominators.add(m);
    maybeCleanup();
  }

  public float getLastDenominator() {
    if (denominators.isEmpty()) return -1;

    return denominators.get(denominators.size() - 1).value;
  }

  public float getLastNumerator() {
    if (numerators.isEmpty()) return 0;

    return numerators.get(numerators.size() - 1).value;
  }

  public float getLastRatio() {
    return getLastNumerator() / getLastDenominator();
  }

  public float getMaxTimeAverage() {
    return getTimeAverage(MAX_AGE);
  }

  public float getTimeAverage(long periodMs) {
    if (periodMs > MAX_AGE) {
      throw new IllegalArgumentException("Bad average period: " + periodMs);
    }

    long now = System.currentTimeMillis();
    float numeratorAvg = recentSum(numerators, periodMs, now) / numerators.size();
    float denominatorAvg = recentSum(denominators, periodMs, now) / denominators.size();
    return numeratorAvg / denominatorAvg;
  }

  private float recentSum(Iterable<Measurement> measurements, long periodMs, long now) {
    float sum = 0.0f;
    for (Measurement m : measurements) {
      if (now - m.timestamp <= periodMs) {
        sum += m.value;
      }
    }
    return sum;
  }

  private void checkType(Measurement m, MeasurementType type) {
    if (m.type != type) {
      throw new IllegalArgumentException("Bad type for measurement " + m);
    }
  }

  private void maybeCleanup() {
    long now = System.currentTimeMillis();
    if (now - lastCleanupTime > MAX_CLEANUP_INTERVAL_MS) {
      removeOld(numerators, now);
      removeOld(denominators, now);
      lastCleanupTime = now;
    }
  }

  private void removeOld(Collection<Measurement> from, long now) {
    for (Iterator<Measurement> it = from.iterator(); it.hasNext(); ) {
      Measurement m = it.next();
      if (now - m.timestamp > MAX_AGE) {
        it.remove();
      }
    }
  }
}
