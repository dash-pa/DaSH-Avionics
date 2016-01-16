package org.dash.avionics.calibration;

import org.androidannotations.api.sharedpreferences.FloatPrefField;
import org.androidannotations.api.sharedpreferences.IntPrefField;
import org.dash.avionics.aircraft.AircraftType;

public class CalibrationProfile {

  private final IntPrefField activeAircraft;
  private final FloatPrefField impellerRatio;
  private final FloatPrefField crankSpeedRatio;
  private final FloatPrefField pilotWeight;

  CalibrationProfile(IntPrefField activeAircraft, FloatPrefField impellerRatio, FloatPrefField
      crankSpeedRatio, FloatPrefField pilotWeight) {
    this.activeAircraft = activeAircraft;
    this.impellerRatio = impellerRatio;
    this.crankSpeedRatio = crankSpeedRatio;
    this.pilotWeight = pilotWeight;
  }

  public AircraftType getActiveAircraft() {
    return AircraftType.values()[activeAircraft.get()];
  }

  public void setActiveAircraft(AircraftType acft) {
    activeAircraft.put(acft.ordinal());
  }

  public float getImpellerRatio() {
    return impellerRatio.get();
  }

  public void setImpellerRatio(float value) {
    impellerRatio.put(value);
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
