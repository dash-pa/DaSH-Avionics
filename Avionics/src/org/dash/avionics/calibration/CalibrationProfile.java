package org.dash.avionics.calibration;

import org.androidannotations.api.sharedpreferences.FloatPrefField;

public class CalibrationProfile {

	private final FloatPrefField propRatio;
	private final FloatPrefField crankSpeedRatio;

	CalibrationProfile(FloatPrefField propRatio, FloatPrefField crankSpeedRatio) {
		this.propRatio = propRatio;
		this.crankSpeedRatio = crankSpeedRatio;
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

}
