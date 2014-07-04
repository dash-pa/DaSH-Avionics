package org.dash.avionics.data;

public class ValueUpdate {
	public ValueUpdate(ValueType type, float value) {
		this.type = type;
		this.value = value;
	}

	public final ValueType type;
	public final float value;

	@Override
	public String toString() {
		return "ValueUpdate [type=" + type + ", value=" + value + "]";
	}
}
