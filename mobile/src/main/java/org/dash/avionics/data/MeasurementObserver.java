package org.dash.avionics.data;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

public class MeasurementObserver extends ContentObserver {

  private final MeasurementListener listener;
  private final ContentResolver resolver;

  public MeasurementObserver(Handler handler, ContentResolver resolver, MeasurementListener listener) {
    super(handler);

    this.resolver = resolver;
    this.listener = listener;
  }

  public void start() {
    Log.i("Observer", "Starting");
    resolver.registerContentObserver(MeasurementStorageColumns.MEASUREMENTS_URI, true, this);
  }

  public void stop() {
    resolver.unregisterContentObserver(this);
  }

  @Override
  public void onChange(boolean selfChange, Uri uri) {
    Cursor cursor = resolver.query(uri, MeasurementStorageColumns.FULL_PROJECTION, null, null, null);
    if (!cursor.moveToFirst()) {
      return;
    }

    int typeIdx = cursor.getColumnIndexOrThrow(MeasurementStorageColumns.VALUE_TYPE);
    int timestampIdx = cursor.getColumnIndexOrThrow(MeasurementStorageColumns.VALUE_TIMESTAMP);
    int valueIdx = cursor.getColumnIndexOrThrow(MeasurementStorageColumns.VALUE);

    MeasurementType type = MeasurementType.values()[cursor.getInt(typeIdx)];
    long timestamp = cursor.getLong(timestampIdx);
    float value = cursor.getFloat(valueIdx);
    cursor.close();

    Measurement measurement = new Measurement(type, value, timestamp);
    listener.onNewMeasurement(measurement);
  }
}
