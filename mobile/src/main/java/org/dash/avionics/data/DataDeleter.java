package org.dash.avionics.data;

import android.content.ContentResolver;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;
import org.dash.avionics.R;

@EBean
public class DataDeleter {
  @RootContext
  Context context;

  public void deleteAllData() {
    ContentResolver contentResolver = context.getContentResolver();

    Log.i("DELETER", "Deleteing ALL data.");
    int deleted = contentResolver.delete(MeasurementStorageColumns.MEASUREMENTS_URI, null, null);
    showSuccess(deleted);
  }

  @UiThread
  void showSuccess(int numDeleted) {
    Log.i("DELETER", numDeleted + " rows were deleted.");
    Toast.makeText(context, R.string.delete_success, Toast.LENGTH_LONG).show();
  }
}
