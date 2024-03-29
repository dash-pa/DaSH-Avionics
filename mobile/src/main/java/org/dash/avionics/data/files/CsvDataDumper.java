package org.dash.avionics.data.files;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.widget.Toast;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;
import org.dash.avionics.R;
import org.dash.avionics.data.MeasurementStorageColumns;
import org.dash.avionics.data.MeasurementType;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@EBean
public class CsvDataDumper {
  private static final SimpleDateFormat TIMESTAMP_FORMAT =
      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

  @RootContext
  Context context;

  private ProgressDialog progress;

  @Background
  public void dumpAllData(FileOutputStream output) {
    ContentResolver contentResolver = context.getContentResolver();

    Cursor cursor = contentResolver.query(
        MeasurementStorageColumns.MEASUREMENTS_URI,
        null, null, null, MeasurementStorageColumns.VALUE_TIMESTAMP);
    if (cursor == null) {
      showFailure();
      return;
    }

    try {
      writeCsvFile(cursor, output);
    } catch (IOException e) {
      Log.w("DUMP", "Failed to write CSV", e);
      showFailure();
      return;
    } finally {
      cursor.close();
    }
    Log.i("DUMP", "Wrote CSV file successfully");
    showSuccess();
  }

  @UiThread
  void showSuccess() {
    Toast.makeText(context, R.string.dump_success, Toast.LENGTH_LONG).show();
  }

  @UiThread
  void showFailure() {
    Toast.makeText(context, R.string.dump_failed, Toast.LENGTH_LONG).show();
  }

  private void writeCsvFile(Cursor cursor, FileOutputStream output) throws IOException {
    try {
      writeCsvHeader(output);
      int idIdx = cursor.getColumnIndexOrThrow(MeasurementStorageColumns._ID);
      int timeIdx = cursor.getColumnIndexOrThrow(MeasurementStorageColumns.VALUE_TIMESTAMP);
      int typeIdx = cursor.getColumnIndexOrThrow(MeasurementStorageColumns.VALUE_TYPE);
      int valueIdx = cursor.getColumnIndexOrThrow(MeasurementStorageColumns.VALUE);
      int lineNumber = 0;
      startProgress();
      while (cursor.moveToNext()) {
        long id = cursor.getLong(idIdx);
        long time = cursor.getLong(timeIdx);
        MeasurementType type = MeasurementType.values()[cursor.getInt(typeIdx)];
        double value = cursor.getDouble(valueIdx);

        writeOneLine(id, time, type, value, output);

        if (++lineNumber % 1000 == 0) {
          updateProgress(lineNumber);
        }
      }
      progressDone();
    } finally {
      if (output != null) output.close();
    }
  }

  @UiThread
  void startProgress() {
    progress = ProgressDialog.show(context, context.getString(R.string.dump_progress_title),
        context.getString(R.string.dump_progress_message, 0), true);
    progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    progress.setProgress(0);
    progress.show();
  }

  @UiThread
  void updateProgress(int lineNumber) {
    Log.i("DUMP", "Wrote " + lineNumber + " rows to file.");
    progress.setMessage(context.getString(R.string.dump_progress_message, lineNumber));
    progress.setProgress(lineNumber);
  }

  @UiThread
  void progressDone() {
    progress.dismiss();
  }

  private void writeCsvHeader(FileOutputStream output) throws IOException {
    StringBuilder builder = new StringBuilder("ID,TIMESTAMP,TYPE");
    for (MeasurementType type : MeasurementType.values()) {
      builder.append(',');
      builder.append(type.name());
    }
    builder.append('\n');
    output.write(builder.toString().getBytes("UTF-8"));
  }

  private void writeOneLine(long id, long time, MeasurementType type, double value, FileOutputStream output) throws IOException {
    StringBuilder builder = new StringBuilder();
    builder.append(id);
    builder.append(',');
    builder.append(formatTimestamp(time));
    builder.append(',');
    builder.append(type.name());
    builder.append(',');

    int valueColumn = type.ordinal();
    for (int i = 0; i < valueColumn; i++) {
      builder.append(',');
    }
    builder.append(value);
    for (int i = valueColumn + 1; i < MeasurementType.values().length; i++) {
      builder.append(',');
    }
    builder.append('\n');
    output.write(builder.toString().getBytes("UTF-8"));
  }

  public String formatTimestamp(long time) {
    Date when = new Date(time);
    return TIMESTAMP_FORMAT.format(when);
  }
}
