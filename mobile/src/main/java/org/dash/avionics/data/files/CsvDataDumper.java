package org.dash.avionics.data.files;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;
import org.dash.avionics.R;
import org.dash.avionics.data.MeasurementStorageColumns;
import org.dash.avionics.data.MeasurementType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@EBean
public class CsvDataDumper {

  @RootContext
  Context context;

  private ProgressDialog progress;

  @Background
  public void dumpAllData() {
    ContentResolver contentResolver = context.getContentResolver();

    Cursor cursor = contentResolver.query(
        MeasurementStorageColumns.MEASUREMENTS_URI,
        null, null, null, MeasurementStorageColumns.VALUE_TIMESTAMP);
    if (cursor == null) {
      showFailure();
      return;
    }

    try {
      writeCsvFile(cursor);
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

  private void writeCsvFile(Cursor cursor) throws IOException {
    FileOutputStream output = null;
    try {
      output = getOutputStream();
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

  private String formatTimestamp(long time) {
    // TODO
    return Long.toString(time);
  }

  private FileOutputStream getOutputStream() throws IOException {
    String storageState = Environment.getExternalStorageState();
    if (!Environment.MEDIA_MOUNTED.equals(storageState)) {
      throw new IOException("External media not available (" + storageState + ")");
    }

    File dir = Environment.getExternalStorageDirectory();
    File file = new File(dir, formatTimestamp(System.currentTimeMillis()) + ".csv");
    if (file.exists()) {
      throw new IOException("File already exists");
    }
    if (!file.createNewFile()) {
      throw new IOException("Unable to create file");
    }
    Log.i("DUMP", "Dumping data to file '" + file.getCanonicalPath() + "'.");
    return new FileOutputStream(file);
  }
}
