package org.dash.avionics.calibration;

import org.androidannotations.annotations.sharedpreferences.DefaultFloat;
import org.androidannotations.annotations.sharedpreferences.SharedPref;

@SharedPref(SharedPref.Scope.UNIQUE)
interface CalibrationStorage {
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
}
