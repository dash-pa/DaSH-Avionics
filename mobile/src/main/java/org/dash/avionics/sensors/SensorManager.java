package org.dash.avionics.sensors;


public interface SensorManager {

	public abstract void connect(SensorListener updater);

	public abstract void disconnect();

}