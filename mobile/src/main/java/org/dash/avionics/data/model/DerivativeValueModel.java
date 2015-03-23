package org.dash.avionics.data.model;

import android.util.Log;

/**
 * Derivative over time (in seconds).
 */
public class DerivativeValueModel implements ValueModel<Float> {
  private long currentValueTime;
  private long previousValueTime;
  private Float currentValue;
  private Float previousValue;

  private final long maxAge;

  public DerivativeValueModel(long maxAge) {
    this.maxAge = maxAge;
  }

  public void addValue(float value) {
    synchronized (this) {
      previousValueTime = currentValueTime;
      previousValue = currentValue;
      currentValue = value;
      currentValueTime = now();
//      Log.v("Deriv", "Adding value " + value + " at " + currentValueTime);
    }
  }

  @Override
  public Float getValue() {
    synchronized (this) {
      float deltaT = (currentValueTime - previousValueTime) / 1000.0f;
      float derivative = (currentValue - previousValue) / deltaT;
//      Log.v("Deriv", "Derivative="+derivative+"; cur=" + currentValue + "; prev=" + previousValue
//          + "; curT=" + currentValueTime + "; prevT=" + previousValueTime);
      return derivative;
    }
  }

  @Override
  public long getValueTime() {
    synchronized (this) {
      return currentValueTime;
    }
  }

  @Override
  public boolean isValid() {
    synchronized (this) {
      long now = now();
      return now - currentValueTime < maxAge &&
          currentValueTime - previousValueTime < maxAge &&
          currentValueTime > previousValueTime;
    }
  }

  protected long now() {
    return System.currentTimeMillis();
  }

}
