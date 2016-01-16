package org.dash.avionics.calibration;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.androidannotations.api.sharedpreferences.FloatPrefField;
import org.androidannotations.api.sharedpreferences.IntPrefField;

@EBean
public class CalibrationManager {
  @Pref
  protected CalibrationStorage_ storage;

  private int lastImpellerIdx = -1;
  private int lastPilotIdx = -1;
  private CalibrationProfile lastProfile;

  public CalibrationProfile loadActiveProfile() {
    return loadProfile(storage.getActiveImpellerProfile().get(), storage.getActivePilotProfile().get
        ());
  }

  public CalibrationProfile loadPilotProfile(int pilotIdx) {
    return loadProfile(storage.getActiveImpellerProfile().get(), pilotIdx);
  }

  public CalibrationProfile loadImpellerProfile(int impellerIdx) {
    return loadProfile(impellerIdx, storage.getActivePilotProfile().get());
  }

  public CalibrationProfile loadProfile(int impellerIdx, int pilotIdx) {
    if (impellerIdx == lastImpellerIdx && pilotIdx == lastPilotIdx) {
      return lastProfile;
    }

    storage.getActiveImpellerProfile().put(impellerIdx);
    storage.getActivePilotProfile().put(pilotIdx);
    lastImpellerIdx = impellerIdx;
    lastPilotIdx = pilotIdx;

    FloatPrefField crankSpeedRatio = storage.crankSpeedRatio();

    FloatPrefField impellerRatio;
    switch (impellerIdx) {
      case 0:
        impellerRatio = storage.impellerSpeedFactor1();
        break;
      case 1:
        impellerRatio = storage.impellerSpeedFactor2();
        break;
      case 2:
        impellerRatio = storage.impellerSpeedFactor3();
        break;
      case 3:
        impellerRatio = storage.impellerSpeedFactor4();
        break;
      default:
        throw new IllegalArgumentException("Bad impeller profile index: "
            + impellerIdx);
    }

    FloatPrefField pilotWeight;
    switch (pilotIdx) {
      case 0:
        pilotWeight = storage.pilot1Weight();
        break;
      case 1:
        pilotWeight = storage.pilot2Weight();
        break;
      case 2:
        pilotWeight = storage.pilot3Weight();
        break;
      default:
        throw new IllegalArgumentException("Bad pilot profile index: "
            + pilotIdx);
    }

    IntPrefField aircraft = storage.getActiveAircraft();
    lastProfile = new CalibrationProfile(aircraft, impellerRatio, crankSpeedRatio, pilotWeight);
    return lastProfile;
  }
}
