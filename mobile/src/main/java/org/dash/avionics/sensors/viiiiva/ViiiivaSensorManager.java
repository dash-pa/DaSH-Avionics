package org.dash.avionics.sensors.viiiiva;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.dash.avionics.data.Measurement;
import org.dash.avionics.data.MeasurementType;
import org.dash.avionics.sensors.SensorListener;
import org.dash.avionics.sensors.SensorManager;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_SINT16;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT16;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8;

/**
 * Manager for the Viiiiva sensor.
 */
@EBean
public class ViiiivaSensorManager extends BluetoothGattCallback
    implements SensorManager, BluetoothAdapter.LeScanCallback {
  private boolean characteristicsEnabled;

  private enum WriteDescriptorStatus {
    FAILURE, DONE, WORKING
  }

  private static final UUID CLIENT_CHARACTERISTIC_CONFIG =
      UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

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

  private final Context context;
  private final BluetoothAdapter btadapter;
  private SensorListener listener = null;
  private BluetoothGatt gatt = null;
  private WriteDescriptorStatus writeStatus = WriteDescriptorStatus.DONE;
  private final Object writeStatusLock = new Object();

  public ViiiivaSensorManager(Context context) {
    this.context = context;
    btadapter = BluetoothAdapter.getDefaultAdapter();
  }

  @Override
  public void connect(SensorListener listener) {
    this.listener = listener;
    startScan();
  }

  @Override
  public void disconnect() {
    //noinspection deprecation
    Log.d("Viiiiva", "Disconnecting");
    btadapter.stopLeScan(this);
    if (gatt != null) {
      gatt.disconnect();
      gatt.close();
    }
    characteristicsEnabled = false;
    setWriteDescriptorStatus(WriteDescriptorStatus.DONE);
  }

  private void startScan() {
    // TODO: Update for API 22.
    Log.d("Viiiiva", "Starting BTLE scan");
    //noinspection deprecation
    btadapter.startLeScan(this);
  }

  @Override
  public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
    if (device == null || device.getName() == null) {
      return;
    }

    Log.d("Viiiiva", "BTLE device: " + device.getName() + " " + device.getAddress());
    if (device.getName().startsWith("Viiiiva")) {
      //noinspection deprecation
      btadapter.stopLeScan(this);
      gatt = device.connectGatt(context, false, this); // set this as the gatt handler
    }
  }

  // sets isConnected and discovers services when connected
  @Override
  public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
    if (newState == BluetoothProfile.STATE_CONNECTED) {
      Log.i("Viiiiva", "BTGatt connected");
      gatt.discoverServices();
    } else {
      Log.w("Viiiiva", "BTGatt state " + newState);
    }
  }

  // enables notification for the power and heart rate characteristics and sets isReady
  @Override
  public void onServicesDiscovered(BluetoothGatt gatt, int status) {
    if (status != BluetoothGatt.GATT_SUCCESS) {
      Log.w("Viiiiva", "BTGatt failed to discover services!");
      return;
    }

    Log.d("Viiiiva", "BTGatt services discovered");
    List<BluetoothGattService> servicesList = gatt.getServices();
    if (servicesList == null) {
      Log.w("Viiiiva", "No services!");
      return;
    }

    // print the services list in an easy-to-read format
    StringBuilder deviceStr = new StringBuilder("Device services (");
    deviceStr.append(servicesList.size());
    deviceStr.append(") {");
    for (BluetoothGattService service : servicesList) {
      deviceStr.append("\n Service:");
      deviceStr.append(service.getUuid());
      deviceStr.append(" {");
      for (BluetoothGattCharacteristic chara : service.getCharacteristics()) {
        deviceStr.append("\n  Chara:");
        deviceStr.append(chara.getUuid());
      }
      deviceStr.append("\n }");
    }
    deviceStr.append("\n}");
    Log.v("Viiiiva", deviceStr.toString());

    synchronized (writeStatusLock) {
      if (!characteristicsEnabled) {
        characteristicsEnabled = true;
        enableCharacteristics();
      }
    }
  }

  @Background
  protected void enableCharacteristics() {
    // enable heart rate notifications
    enableCharacteristic(HR_SERVICE_UUID, HEART_RATE_CHARACTERISTIC, "heart rate");

    // enable power notifications
    enableCharacteristic(POW_SERVICE_UUID, POWER_CHARACTERISTIC, "power");
  }

  private void enableCharacteristic(UUID serviceUuid, UUID characteristicUuid, String serviceName) {
    BluetoothGattService service = gatt.getService(serviceUuid);
    if (service == null) {
      Log.w("Viiiiva", serviceName + " service is null!");
      return;
    }

    BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
    if (characteristic == null) {
      Log.w("Viiiiva", serviceName + " characteristic is null!");
      return;
    }

    // Do some sort of client-side notification configuration changing
    gatt.setCharacteristicNotification(characteristic, true);

    // Create a Characteristic Configuration descriptor
    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
    if (descriptor == null) {
      List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
      StringBuilder errStr = new StringBuilder("Descriptor is null for service ");
      errStr.append(serviceName);
      errStr.append("\n  Available descriptors: ");
      for (BluetoothGattDescriptor desc : descriptors) {
        if (desc != null) {
          errStr.append(desc.getUuid());
          errStr.append(", ");
        }
      }

      Log.w("Viiiiva", errStr.toString());
      return;
    }

    // Set Enable or Disable notification on the descriptor depending on argument val
    Log.v("Viiiiva", "Enabling notifications for " + descriptor.getUuid());
    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

    // Wait for any possible descriptor writing to finish
    waitForWriteDescriptorNotWorking();
    setWriteDescriptorStatus(WriteDescriptorStatus.WORKING);

    if (!gatt.writeDescriptor(descriptor)) { // failed to initiate descriptor write
      // We have to free this up for future calls to setNotifyValue to work
      setWriteDescriptorStatus(WriteDescriptorStatus.DONE);
      Log.w("Viiiiva", serviceName + " descriptor write failed.");
      return;
    }

    // Wait for the descriptor to be written
    waitForWriteDescriptorNotWorking();

    // If there was a failure, this would be equal to WD_STATUS_FAILURE instead of WD_STATUS_DONE
    if (writeStatus != WriteDescriptorStatus.DONE) {
      // Done writing descriptors
      setWriteDescriptorStatus(WriteDescriptorStatus.DONE);
      Log.w("Viiiiva", serviceName + " descriptor write failed with status " + writeStatus);
      return;
    }

    Log.i("Viiiiva", "enabled " + serviceName + " notifications");
  }

  @Override
  public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                int status) {
    if (status == BluetoothGatt.GATT_SUCCESS) {
      setWriteDescriptorStatus(WriteDescriptorStatus.DONE);
    } else {
      setWriteDescriptorStatus(WriteDescriptorStatus.FAILURE);
    }
  }

  private void setWriteDescriptorStatus(WriteDescriptorStatus status) {
    synchronized (writeStatusLock) {
      writeStatus = status;
      writeStatusLock.notifyAll();
    }
  }

  private void waitForWriteDescriptorNotWorking() {
    synchronized (writeStatusLock) {
      Log.v("Viiiiva", "Starting wait for not working: " + writeStatus);
      while (writeStatus == WriteDescriptorStatus.WORKING) {
        try {
          writeStatusLock.wait();
        } catch (InterruptedException e) {
          // Try again.
        }
      }
      Log.v("Viiiiva", "Done wait for not working: " + writeStatus);
    }
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

    listener.onNewMeasurement(new Measurement(MeasurementType.HEART_BEAT, heartRate));
  }

  // parse power data from the characteristic and set the powerMeter variable
  // https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.cycling_power_measurement.xml
  private void handlePowerData(BluetoothGattCharacteristic characteristic) {
    // The properties for this characteristic are 16-bits long, as opposed to the heart rate's properties, which are 8-bits long
    // The power measurement is after the 16-bit properties value, so use a 2-byte offset
    Integer power = characteristic.getIntValue(FORMAT_SINT16, 2); // Units are Watts
    if (power == null) {
      Log.w("Viiiiva", "Invalid heart rate value " + Arrays.toString(characteristic.getValue()));
      return;
    }

    listener.onNewMeasurement(new Measurement(MeasurementType.POWER, power));
  }

}
