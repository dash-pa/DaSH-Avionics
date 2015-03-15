package org.dash.avionics.calibration;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.androidannotations.api.sharedpreferences.FloatPrefField;
import org.androidannotations.api.sharedpreferences.IntPrefField;

@EBean
public class CalibrationManager {
  @Pref
  protected CalibrationStorage_ storage;

  public CalibrationProfile loadActiveProfile() {
    return loadProfile(storage.getActivePropProfile().get(), storage.getActivePilotProfile().get());
  }

  public CalibrationProfile loadPilotProfile(int pilotIdx) {
    return loadProfile(storage.getActivePropProfile().get(), pilotIdx);
  }

  public CalibrationProfile loadPropProfile(int propIdx) {
    return loadProfile(propIdx, storage.getActivePilotProfile().get());
  }

  public CalibrationProfile loadProfile(int propIdx, int pilotIdx) {
    storage.getActivePropProfile().put(propIdx);
    storage.getActivePilotProfile().put(pilotIdx);

    FloatPrefField crankSpeedRatio = storage.crankSpeedRatio();

    FloatPrefField propRatio;
    switch (propIdx) {
      case 0:
        propRatio = storage.propSpeedFactor1();
        break;
      case 1:
        propRatio = storage.propSpeedFactor2();
        break;
      case 2:
        propRatio = storage.propSpeedFactor3();
        break;
      case 3:
        propRatio = storage.propSpeedFactor4();
        break;
      default:
        throw new IllegalArgumentException("Bad prop profile index: "
            + propIdx);
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
            + propIdx);
    }

    IntPrefField aircraft = storage.getActiveAircraft();
    return new CalibrationProfile(aircraft, propRatio, crankSpeedRatio, pilotWeight);
  }
}
