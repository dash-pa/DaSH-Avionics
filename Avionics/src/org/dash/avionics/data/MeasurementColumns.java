package org.dash.avionics.data;

import android.provider.BaseColumns;

public interface MeasurementColumns extends BaseColumns {

	static final String VALUE_TIMESTAMP = "timestamp";
	static final String VALUE_TYPE = "type";
	static final String VALUE = "value";

	static final String MEASUREMENT_TABLE_NAME = "measurements";

	static final String MEASUREMENT_TABLE_CREATE =
		"CREATE TABLE " + MEASUREMENT_TABLE_NAME + "(" +
				MeasurementColumns.VALUE_TIMESTAMP + " INTEGER NOT NULL, " +
				MeasurementColumns.VALUE_TYPE + " INTEGER NOT NULL, " +
				MeasurementColumns.VALUE + " REAL NOT NULL";

}
