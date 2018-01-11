package org.dash.avionics.sensors.disto;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.dash.avionics.data.Measurement;
import org.dash.avionics.data.MeasurementType;
import org.dash.avionics.sensors.btle.BTLESensorManager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

/**
 * Manager for the WeatherMeter sensor.
 */
@EBean
public class DistoSensorManager extends BTLESensorManager {

  private static final UUID DIST_SERVICE_UUID =
          UUID.fromString("3ab10100-f831-4395-b29d-570977d5bf94");
  private static final UUID CHARACTERISTIC_DISTANCE_UUID  =
          UUID.fromString("3ab10101-f831-4395-b29d-570977d5bf94");
  public static final UUID CHARACTERISTIC_DISTANCE_UNIT_UUID =
          UUID.fromString("3ab10102-f831-4395-b29d-570977d5bf94");
  public static final UUID CHARACTERISTIC_UNKNOWN1 =
          UUID.fromString("3ab10109-f831-4395-b29d-570977d5bf94");
  public static final UUID CHARACTERISTIC_UNKNOWN2 =
          UUID.fromString("3ab1010a-f831-4395-b29d-570977d5bf94");
  public static final UUID CHARACTERISTIC_UNKNOWN3 =
          UUID.fromString("3ab1010c-f831-4395-b29d-570977d5bf94");

  public DistoSensorManager(Context context) {
    super(context, "DISTO");
  }

  @Background
  @Override
  protected void enableCharacteristics() {
    enableCharacteristic(DIST_SERVICE_UUID, CHARACTERISTIC_DISTANCE_UUID, "dist height", true);
  }

  @Override
  public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
    if (characteristic.getUuid().equals(CHARACTERISTIC_DISTANCE_UUID)) {
      handleHeightData(characteristic);
    } else {
      Log.w("DISTO", "Unknown Charactaristic " + characteristic.getUuid()
              + " with value " + characteristic.getValue());
    }
  }


  private void handleHeightData(BluetoothGattCharacteristic characteristic) {
    if (CHARACTERISTIC_DISTANCE_UUID.equals(characteristic.getUuid())) {
      ByteBuffer buffer = ByteBuffer.wrap(characteristic.getValue());
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      float height =  buffer.getFloat();
//      Log.v("DISTO", "Height value of " + height);
      getListener().onNewMeasurement(new Measurement(getMeasurmentType(), height));
    }
  }

  protected MeasurementType getMeasurmentType() {
    return MeasurementType.HEIGHT;
  }
}
