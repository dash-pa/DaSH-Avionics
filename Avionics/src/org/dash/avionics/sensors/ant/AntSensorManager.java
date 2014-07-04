package org.dash.avionics.sensors.ant;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.dash.avionics.sensors.SensorManager;
import org.dash.avionics.sensors.ValueUpdater;

@EBean
public class AntSensorManager implements SensorManager {
	// Delegate to a package-protected class to hide the interface ugliness.
	@Bean
	protected AntSensorImpl impl;

	@Override
	public void connect(ValueUpdater updater) {
		impl.connect(updater);
	}

	@Override
	public void disconnect() {
		impl.disconnect();
	}
}
