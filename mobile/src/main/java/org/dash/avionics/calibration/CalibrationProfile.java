package org.dash.avionics.calibration;

import org.androidannotations.api.sharedpreferences.FloatPrefField;
import org.androidannotations.api.sharedpreferences.IntPrefField;
import org.dash.avionics.alerts.CruiseSpeedCalculator.Aircraft;

public class CalibrationProfile {

  private final IntPrefField activeAircraft;
  private final FloatPrefField propRatio;
  private final FloatPrefField crankSpeedRatio;
  private final FloatPrefField pilotWeight;

  CalibrationProfile(IntPrefField activeAircraft, FloatPrefField propRatio, FloatPrefField crankSpeedRatio, FloatPrefField pilotWeight) {
    this.activeAircraft = activeAircraft;
    this.propRatio = propRatio;
    this.crankSpeedRatio = crankSpeedRatio;
    this.pilotWeight = pilotWeight;
  }

  public Aircraft getActiveAircraft() {
    return Aircraft.values()[activeAircraft.get()];
  }

  public void setActiveAircraft(Aircraft acft) {
    activeAircraft.put(acft.ordinal());
  }

  public float getPropRatio() {
    return propRatio.get();
  }

  public void setPropRatio(float value) {
    propRatio.put(value);
  }

  public float getCrankSpeedRatio() {
    return crankSpeedRatio.get();
  }

  public void setCrankSpeedRatio(float value) {
    crankSpeedRatio.put(value);
  }

  public float getPilotWeight() {
    return pilotWeight.get();
  }

  public void setPilotWeight(float weight) {
    pilotWeight.put(weight);
  }
}
