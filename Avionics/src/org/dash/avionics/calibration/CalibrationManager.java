package org.dash.avionics.calibration;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.androidannotations.api.sharedpreferences.FloatPrefField;

@EBean
public class CalibrationManager {
	@Pref
	protected CalibrationStorage_ storage;

	public CalibrationProfile getProfile(int profileIdx) {
		FloatPrefField propRatio;
		FloatPrefField crankSpeedRatio = storage.crankSpeedRatio();

		switch (profileIdx) {
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
			throw new IllegalArgumentException("Bad profile index: "
					+ profileIdx);
		}

		return new CalibrationProfile(propRatio, crankSpeedRatio);
	}
}
