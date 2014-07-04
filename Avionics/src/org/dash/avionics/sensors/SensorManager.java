package org.dash.avionics.sensors;

import org.dash.avionics.data.MeasurementListener;

public interface SensorManager {

	public abstract void connect(MeasurementListener updater);

	public abstract void disconnect();

}