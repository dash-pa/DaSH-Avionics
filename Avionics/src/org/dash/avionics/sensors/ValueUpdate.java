package org.dash.avionics.sensors;

public class ValueUpdate {
	public ValueUpdate(ValueType type, int value) {
		this.type = type;
		this.value = value;
	}

	public final ValueType type;
	public final int value;

	@Override
	public String toString() {
		return "ValueUpdate [type=" + type + ", value=" + value + "]";
	}
}
