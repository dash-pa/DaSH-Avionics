package org.dash.avionics.data;

public class Measurement {

	public Measurement(MeasurementType type, float value) {
		this.type = type;
		this.value = value;
		this.timestamp = System.currentTimeMillis();
	}

	public Measurement(MeasurementType type, float value, long timestamp) {
		this.type = type;
		this.value = value;
		this.timestamp = timestamp;
	}

	public final MeasurementType type;
	public final float value;
	public final long timestamp;

	@Override
	public String toString() {
		return "ValueUpdate [type=" + type + ", value=" + value
				+ ", timestamp=" + timestamp + "]";
	}
}
