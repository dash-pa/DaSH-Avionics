package org.dash.avionics.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import org.androidannotations.annotations.EProvider;

@EProvider
public class MeasurementContentProvider extends ContentProvider {

  public enum UrlType {
    MEASUREMENTS,
    MEASUREMENT_ID,
    MEASUREMENT_TYPE,
  }

  private static final String AUTHORITY = "org.dash.avionics";

  private UriMatcher uriMatcher;
  private SQLiteDatabase db;

  @Override
  public boolean onCreate() {
    uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    uriMatcher.addURI(AUTHORITY, MeasurementStorageColumns.MEASUREMENT_TABLE_NAME, UrlType.MEASUREMENTS.ordinal());
    uriMatcher.addURI(AUTHORITY, MeasurementStorageColumns.MEASUREMENT_TABLE_NAME + "/#", UrlType.MEASUREMENT_ID.ordinal());
    for (MeasurementType type : MeasurementType.values()) {
      uriMatcher.addURI(AUTHORITY, MeasurementStorageColumns.MEASUREMENT_TABLE_NAME + "/" + type.name(), UrlType.MEASUREMENT_TYPE.ordinal());
    }

    MeasurementStorage storage = new MeasurementStorage(getContext());
    db = storage.getWritableDatabase();

    return db != null;
  }

  @Override
  public String getType(Uri uri) {
    UrlType type = getUriType(uri);
    switch (type) {
      case MEASUREMENTS:
        return MeasurementStorageColumns.CONTENT_TYPE;
      case MEASUREMENT_ID:
        return MeasurementStorageColumns.ITEM_TYPE;
      case MEASUREMENT_TYPE:
        return MeasurementStorageColumns.CONTENT_TYPE;
      default:
        throw new IllegalArgumentException("Bad type in URI: " + uri);
    }
  }

  private UrlType getUriType(Uri uri) {
    return UrlType.values()[uriMatcher.match(uri)];
  }

  @Override
  public int delete(Uri uri, String where, String[] selectionArgs) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    if (getUriType(uri) != UrlType.MEASUREMENTS) {
      Log.e("Content", "Tried to insert into bad URI: " + uri);
      return null;
    }

    long insertedId = -1;
    try {
      db.beginTransaction();
      insertedId = db.insert(MeasurementStorageColumns.MEASUREMENT_TABLE_NAME, null, values);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }

    Uri result = null;
    if (insertedId >= 0) {
      result = ContentUris.appendId(MeasurementStorageColumns.MEASUREMENTS_URI.buildUpon(), insertedId).build();
      getContext().getContentResolver().notifyChange(result, null, false);
    }
    return result;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection,
                    String[] selectionArgs) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection,
                      String[] selectionArgs, String sortOrder) {
    SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
    builder.setTables(MeasurementStorageColumns.MEASUREMENT_TABLE_NAME);
    switch (getUriType(uri)) {
      case MEASUREMENTS:
        break;
      case MEASUREMENT_ID:
        builder.appendWhere(MeasurementStorageColumns._ID + " = " + uri.getPathSegments().get(1));
        break;
      case MEASUREMENT_TYPE: {
        MeasurementType type = MeasurementType.valueOf(uri.getPathSegments().get(1));
        builder.appendWhere(MeasurementStorageColumns.VALUE_TYPE + " = " + type.ordinal());
        break;
      }
      default:
        throw new IllegalArgumentException("Bad type in URI: " + uri);
    }

    Cursor cursor = builder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
    cursor.setNotificationUri(getContext().getContentResolver(), uri);
    return cursor;
  }

}
