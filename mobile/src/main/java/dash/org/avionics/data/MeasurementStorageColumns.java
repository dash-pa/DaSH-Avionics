package org.dash.avionics.data;

import android.net.Uri;
import android.provider.BaseColumns;

public interface MeasurementStorageColumns extends BaseColumns {

	// Fields.
	public static final String VALUE_TIMESTAMP = "timestamp";
	public static final String VALUE_TYPE = "type";
	public static final String VALUE = "value";

	static final String MEASUREMENT_TABLE_NAME = "measurements";

	static final String MEASUREMENT_TABLE_CREATE =
		"CREATE TABLE " + MEASUREMENT_TABLE_NAME + "(" +
				_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				VALUE_TIMESTAMP + " INTEGER NOT NULL, " +
				VALUE_TYPE + " INTEGER NOT NULL, " +
				VALUE + " REAL NOT NULL);";

	public static final String FULL_PROJECTION[] = {
		_ID, VALUE_TIMESTAMP, VALUE_TYPE, VALUE,
	};

	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.dash.measurements";
	public static final String ITEM_TYPE = "vnd.android.cursor.item/vnd.dash.measurement";

	public static final Uri MEASUREMENTS_URI =
			Uri.parse("content://org.dash.avionics/" + MEASUREMENT_TABLE_NAME);
}
