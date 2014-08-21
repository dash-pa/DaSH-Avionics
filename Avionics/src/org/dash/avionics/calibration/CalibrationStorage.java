package org.dash.avionics.calibration;

import org.androidannotations.annotations.sharedpreferences.DefaultFloat;
import org.androidannotations.annotations.sharedpreferences.DefaultInt;
import org.androidannotations.annotations.sharedpreferences.SharedPref;
import org.dash.avionics.alerts.CruiseSpeedCalculator;

@SharedPref(SharedPref.Scope.UNIQUE)
interface CalibrationStorage {

	@DefaultInt(0)
	int getActiveAircraft();

	@DefaultInt(0)
	int getActivePropProfile();
	@DefaultFloat(0.01375f)
	float propSpeedFactor1();
	@DefaultFloat(0.01375f)
	float propSpeedFactor2();
	@DefaultFloat(0.01375f)
	float propSpeedFactor3();
	@DefaultFloat(0.01375f)
	float propSpeedFactor4();

	@DefaultFloat(1.0f)  // TODO
	float crankSpeedRatio();

	@DefaultInt(0)
	int getActivePilotProfile();
	@DefaultFloat(CruiseSpeedCalculator.LIGHT_PILOT_WEIGHT_KG)
	float pilot1Weight();
	@DefaultFloat(CruiseSpeedCalculator.MEDIUM_PILOT_WEIGHT_KG)
	float pilot2Weight();
	@DefaultFloat(CruiseSpeedCalculator.HEAVY_PILOT_WEIGHT_KG)
	float pilot3Weight();
}
