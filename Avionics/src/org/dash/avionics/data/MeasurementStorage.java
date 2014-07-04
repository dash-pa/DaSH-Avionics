package org.dash.avionics.data;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

@EBean
public class MeasurementStorage extends SQLiteOpenHelper {

	private static final String MEASUREMENT_DB_NAME = "measurements";
	private static final int MEASUREMENT_DB_VERSION = 1;

	public MeasurementStorage(Context context) {
		super(context, MEASUREMENT_DB_NAME, null, MEASUREMENT_DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(MeasurementColumns.MEASUREMENT_TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		throw new UnsupportedOperationException("Don't know how to upgrade");
	}

	@Background(id="storage-insert")
	public void insertMeasurement(ValueUpdate measurement) {
		ContentValues values = new ContentValues();
		values.put(MeasurementColumns.VALUE_TIMESTAMP, measurement.timestamp);
		values.put(MeasurementColumns.VALUE_TYPE, measurement.type.ordinal());
		values.put(MeasurementColumns.VALUE, measurement.value);
		getWritableDatabase().insertOrThrow(MeasurementColumns.MEASUREMENT_TABLE_NAME, null, values);
	}
}
