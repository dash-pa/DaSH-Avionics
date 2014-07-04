package org.dash.avionics.sensors;

public interface SensorManager {

	public abstract void connect(ValueUpdater updater);

	public abstract void disconnect();

}