package org.dash.avionics.sensors;

import org.dash.avionics.data.ValueUpdate;

public interface ValueUpdater {
	void updateValue(ValueUpdate update);
}