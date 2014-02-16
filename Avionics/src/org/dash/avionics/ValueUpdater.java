package org.dash.avionics;

interface ValueUpdater {
	void updateValue(ValueType valueType, int value);
}