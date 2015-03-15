package org.dash.avionics.sensors;

import org.dash.avionics.data.Measurement;

public interface SensorListener {
	void onNewMeasurement(Measurement measurement);
}