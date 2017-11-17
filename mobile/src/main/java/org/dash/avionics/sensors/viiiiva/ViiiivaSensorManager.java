package org.dash.avionics.sensors.viiiiva;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.dash.avionics.data.Measurement;
import org.dash.avionics.data.MeasurementType;
import org.dash.avionics.sensors.btle.BTLESensorManager;

import java.util.Arrays;
import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_SINT16;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT16;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8;

/**
 * Manager for the Viiiiva sensor.
 */
@EBean
public class ViiiivaSensorManager extends BTLESensorManager {
  private static final UUID HR_SERVICE_UUID =
      UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
  private static final UUID HEART_RATE_CHARACTERISTIC =
      UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
  // https://developer.bluetooth.org/gatt/services/Pages/ServiceViewer.aspx?u=org.bluetooth.service.cycling_power.xml
  private static final UUID POW_SERVICE_UUID =
      UUID.fromString("00001818-0000-1000-8000-00805f9b34fb");
  //  https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.cycling_power_measurement.xml
  private static final UUID POWER_CHARACTERISTIC =
      UUID.fromString("00002a63-0000-1000-8000-00805f9b34fb");

  // https://developer.bluetooth.org/gatt/services/Pages/ServiceViewer.aspx?u=org.bluetooth.service.cycling_speed_and_cadence.xml
  private static final UUID CADENCE_SERVICE_UUID =
      UUID.fromString("00001816-0000-1000-8000-00805f9b34fb");
  // https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.csc_measurement.xml
  private static final UUID CADENCE_CHARACTERISTIC =
      UUID.fromString("00002a5b-0000-1000-8000-00805f9b34fb");

  private Integer previousCrankEvent;

  public ViiiivaSensorManager(Context context) {
    super(context, "Viiiiva");
  }

  @Background
  @Override
  protected void enableCharacteristics() {
    previousCrankEvent = null;

    // enable heart rate notifications
    enableCharacteristic(HR_SERVICE_UUID, HEART_RATE_CHARACTERISTIC, "heart rate");

    // enable power notifications
    enableCharacteristic(POW_SERVICE_UUID, POWER_CHARACTERISTIC, "power");

    // enable cadence notifications
    // TODO(Tony): Enable and test
    enableCharacteristic(CADENCE_SERVICE_UUID, CADENCE_CHARACTERISTIC, "cadence");
  }

  @Override
  public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
    if (status == BluetoothGatt.GATT_SUCCESS) {
      onCharacteristicChanged(gatt, characteristic);
    }
  }

  @Override
  public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
    if (characteristic.getUuid().equals(HEART_RATE_CHARACTERISTIC)) {
      handleHeartData(characteristic);
    } else if (characteristic.getUuid().equals(POWER_CHARACTERISTIC)) {
      handlePowerData(characteristic);
    } else if (characteristic.getUuid().equals(CADENCE_CHARACTERISTIC)) {
      handleCadenceData(characteristic);
    } else {
      Log.w("Viiiiva", "Got unknown characteristic notification: " + characteristic.getUuid());
    }
  }

  // parse heart rate data from the characteristic and set the heartRate variable
  // https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
  private void handleHeartData(BluetoothGattCharacteristic characteristic) {
    Log.v("Viiiiva", "New heart rate notification");
    // The heart rate measurement can be either a unsigned 8-bit int, or an unsigned 16-bit int
    int flag = characteristic.getProperties();
    int format;
    if ((flag & 0x01) != 0) {
      format = FORMAT_UINT16;
    } else {
      format = FORMAT_UINT8;
    }

    // The heart rate measurement is after the 8-bit properties value, so use a 1-byte offset
    Integer heartRate = characteristic.getIntValue(format, 1); // Units are Beats per Minute (BPM)
    if (heartRate == null) {
      Log.w("Viiiiva", "Invalid heart rate value " + Arrays.toString(characteristic.getValue()));
      return;
    }
    Log.v("Viiiiva", "Reporting heart rate " + heartRate);

    getListener().onNewMeasurement(new Measurement(MeasurementType.HEART_BEAT, heartRate));
  }

  // https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.cycling_power_measurement.xml
  private void handlePowerData(BluetoothGattCharacteristic characteristic) {
    // The properties for this characteristic are 16-bits long, as opposed to the heart rate's properties, which are 8-bits long
    // The power measurement is after the 16-bit properties value, so use a 2-byte offset
    Integer power = characteristic.getIntValue(FORMAT_SINT16, 2); // Units are Watts
    if (power == null) {
      Log.w("Viiiiva", "Invalid heart rate value " + Arrays.toString(characteristic.getValue()));
      return;
    }

    getListener().onNewMeasurement(new Measurement(MeasurementType.POWER, power));
  }

  // parse cadence data from the characteristic
  // https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.csc_measurement.xml
  private void handleCadenceData(BluetoothGattCharacteristic characteristic) {
    // Before the last crank event, we had:
    // Offset 0 - flags (1 byte)
    // Offset 1 - Cumulative wheel revolutions (4 bytes)
    // Offset 5 - Last wheel event time (2 bytes)
    // Offset 7 - Cumulative crank revolutions (2 bytes)
    Integer flags = characteristic.getIntValue(FORMAT_UINT8, 0);
    int offset = 9;

    //Flags bit 2 indicates if crank data is present
    if ((flags & 0x02) == 0) {
      Log.w("Viiiiva", "No crank data availible");
      return;
    }
    //If the first bit is 0, we don't have wheel data and use a smaller offset for crank data.
    if ((flags & 0x01) == 0) {
      offset = 3;
    }

    Integer lastCrankEvent = characteristic.getIntValue(FORMAT_UINT16, offset);
    if (lastCrankEvent == null) {
      Log.w("Viiiiva", "Invalid crank event time " + Arrays.toString(characteristic.getValue()));
      return;
    }

    if (previousCrankEvent != null) {
      if (previousCrankEvent > lastCrankEvent) {
        // The time rolled over, go negative to get a sensible diff.
        previousCrankEvent -= 0xFFFF;
      }

      float crankPeriodSeconds =
          (lastCrankEvent.floatValue() - previousCrankEvent.floatValue()) / 1024.0f;
      float crankFrequencyRpm = 0;

      if (crankPeriodSeconds != 0) {
        crankFrequencyRpm = 60.0f / crankPeriodSeconds;
      }

      getListener().onNewMeasurement(new Measurement(MeasurementType.CRANK_RPM, crankFrequencyRpm));
    }
    previousCrankEvent = lastCrankEvent;
  }
}
