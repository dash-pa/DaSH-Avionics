package org.dash.avionics.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.androidannotations.annotations.EBean;

@EBean
class MeasurementStorage extends SQLiteOpenHelper {

  private static final String MEASUREMENT_DB_NAME = "measurements";
  private static final int MEASUREMENT_DB_VERSION = 1;

  public MeasurementStorage(Context context) {
    super(context, MEASUREMENT_DB_NAME, null, MEASUREMENT_DB_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(MeasurementStorageColumns.MEASUREMENT_TABLE_CREATE);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    throw new UnsupportedOperationException("Don't know how to upgrade");
  }
}
