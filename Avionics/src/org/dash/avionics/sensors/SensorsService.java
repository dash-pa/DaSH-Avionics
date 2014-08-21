package org.dash.avionics.sensors;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EService;
import org.dash.avionics.data.Measurement;
import org.dash.avionics.data.MeasurementStorageColumns;
import org.dash.avionics.sensors.ant.AntSensorManager;
import org.dash.avionics.sensors.arduino.ArduinoSensorManager;
import org.dash.avionics.sensors.fake.FakeSensorManager;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.os.IBinder;

@EService
public class SensorsService extends Service implements SensorListener {
	private static final boolean USE_FAKE_DATA = true;

	/*
	 * Managers for many types of sensors.
	 */
	@Bean
	protected ArduinoSensorManager arduinoSensor;
	@Bean
	protected AntSensorManager antSensor;
	@Bean
	protected FakeSensorManager fakeSensor;

	private ContentResolver contentResolver;

	@Override
	public void onCreate() {
		contentResolver = getContentResolver();

		if (USE_FAKE_DATA) {
			fakeSensor.connect(this);
		} else {
			antSensor.connect(this);
			arduinoSensor.connect(this);
		}
	}

	@Override
	public void onDestroy() {
		if (USE_FAKE_DATA) {
			fakeSensor.disconnect();
		} else {
			antSensor.disconnect();
			arduinoSensor.disconnect();
		}
	}

	@Override
	public void onNewMeasurement(Measurement update) {
		ContentValues values = new ContentValues();
		values.put(MeasurementStorageColumns.VALUE_TYPE, update.type.ordinal());
		values.put(MeasurementStorageColumns.VALUE_TIMESTAMP, update.timestamp);
		values.put(MeasurementStorageColumns.VALUE, update.value);
		contentResolver.insert(MeasurementStorageColumns.MEASUREMENTS_URI, values);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
