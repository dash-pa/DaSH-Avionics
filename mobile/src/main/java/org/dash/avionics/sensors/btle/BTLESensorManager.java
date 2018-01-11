package org.dash.avionics.sensors.btle;

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

import org.dash.avionics.sensors.SensorListener;
import org.dash.avionics.sensors.SensorManager;

import java.util.List;
import java.util.UUID;

/**
 * Base class for BTLE sensor managers.
 */
public abstract class BTLESensorManager extends BluetoothGattCallback
    implements SensorManager, BluetoothAdapter.LeScanCallback {
  private enum WriteDescriptorStatus {
    FAILURE, DONE, WORKING
  }

  private static final UUID CLIENT_CHARACTERISTIC_CONFIG =
      UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

  private final Context context;
  private final String deviceNamePrefix;
  private final BluetoothAdapter btadapter;
  private SensorListener listener = null;
  private BluetoothGatt gatt = null;
  private WriteDescriptorStatus writeStatus = WriteDescriptorStatus.DONE;
  private final Object writeStatusLock = new Object();
  private boolean characteristicsEnabled;

  public BTLESensorManager(Context context, String deviceNamePrefix) {
    this.context = context;
    this.deviceNamePrefix = deviceNamePrefix;
    btadapter = BluetoothAdapter.getDefaultAdapter();
  }

  @Override
  public void connect(SensorListener listener) {
    this.listener = listener;
    startScan();
  }

  protected SensorListener getListener() {
    return listener;
  }

  @Override
  public void disconnect() {
    //noinspection deprecation
    Log.d("BTLE", "Disconnecting");
    if (btadapter != null) {
      btadapter.stopLeScan(this);
    }
    closegatt(gatt);
  }

  private void closegatt(BluetoothGatt gatt) {
    if (gatt != null) {
      gatt.disconnect();
      gatt.close();;
    }
    synchronized (writeStatusLock) {
      characteristicsEnabled = false;
      setWriteDescriptorStatus(WriteDescriptorStatus.DONE);
    }

  }

  private void startScan() {
    // TODO: Update for API 22.
    Log.d("BTLE", "Starting BTLE scan");
    //noinspection deprecation
    if (btadapter != null) {
      btadapter.startLeScan(this);
    }
  }

  @Override
  public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
    if (device == null || device.getName() == null) {
      return;
    }

//    Log.d("BTLE", "BTLE device: " + device.getName() + ",  Address: " + device.getAddress());
//    Log.d("BTLE", "Searching for device with prefix " + deviceNamePrefix);
    if (!characteristicsEnabled && device.getName().startsWith(deviceNamePrefix) && onDeviceFound(device)) {
      gatt = device.connectGatt(context, false, this); // set this as the gatt handler
    }
  }

  /**
   * Allows the Implementing SensorManager class to respond and possibly reject a found device
   * based on properties beyond the device name.
   * @param device
   * @return boolean true if discovered device is acceptable.
   */
  protected boolean onDeviceFound(BluetoothDevice device) {
    return true;
  }

  // sets isConnected and discovers services when connected
  @Override
  public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
    if (newState == BluetoothProfile.STATE_CONNECTED) {
        Log.i("BTLE", "BTGatt connected");
        gatt.discoverServices();
    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
      //Reconnect if we were disconnected but this is the correct device.
        closegatt(gatt);
        Log.i("BTLE", "BTGatt disconnected");
    } else {
      Log.w("BTLE", "BTGatt state " + newState);
    }
  }

  // enables notification for the power and heart rate characteristics and sets isReady
  @Override
  public void onServicesDiscovered(BluetoothGatt gatt, int status) {
    if (status != BluetoothGatt.GATT_SUCCESS) {
      Log.w("BTLE", "BTGatt failed to discover services!");
      return;
    }

    Log.d("BTLE", "BTGatt services discovered");
    List<BluetoothGattService> servicesList = gatt.getServices();
    if (servicesList == null) {
      Log.w("BTLE", "No services!");
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
    Log.v("BTLE", deviceStr.toString());

    synchronized (writeStatusLock) {
      if (!characteristicsEnabled) {
        characteristicsEnabled = true;
        enableCharacteristics();
      }
    }
  }

  protected abstract void enableCharacteristics();

  protected void enableCharacteristic(UUID serviceUuid, UUID characteristicUuid, String
          serviceName) {
    enableCharacteristic(serviceUuid, characteristicUuid, serviceName, false);
  }

  protected void enableCharacteristic(UUID serviceUuid, UUID characteristicUuid, String
      serviceName, boolean enableIndication) {
    BluetoothGattService service = gatt.getService(serviceUuid);
    if (service == null) {
      Log.w("BTLE", serviceName + " service is null!");
      return;
    }

    BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
    if (characteristic == null) {
      Log.w("BTLE", serviceName + " characteristic is null!");
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

      Log.w("BTLE", errStr.toString());
      return;
    }

    // Set Enable or Disable notification on the descriptor depending on argument val
    Log.v("BTLE", "Enabling notifications for " + descriptor.getUuid());
    if (enableIndication) {
      descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
    } else {
      descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
    }

    // Wait for any possible descriptor writing to finish
    waitForWriteDescriptorNotWorking();
    setWriteDescriptorStatus(WriteDescriptorStatus.WORKING);

    if (!gatt.writeDescriptor(descriptor)) { // failed to initiate descriptor write
      // We have to free this up for future calls to setNotifyValue to work
      setWriteDescriptorStatus(WriteDescriptorStatus.DONE);
      Log.w("BTLE", serviceName + " descriptor write failed.");
      return;
    }

    // Wait for the descriptor to be written
    waitForWriteDescriptorNotWorking();

    // If there was a failure, this would be equal to WD_STATUS_FAILURE instead of WD_STATUS_DONE
    if (writeStatus != WriteDescriptorStatus.DONE) {
      // Done writing descriptors
      setWriteDescriptorStatus(WriteDescriptorStatus.DONE);
      Log.w("BTLE", serviceName + " descriptor write failed with status " + writeStatus);
      return;
    }

    Log.i("BTLE", "enabled " + serviceName + " notifications");
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
      Log.v("BTLE", "Starting wait for not working: " + writeStatus);
      while (writeStatus == WriteDescriptorStatus.WORKING) {
        try {
          writeStatusLock.wait();
        } catch (InterruptedException e) {
          // Try again.
        }
      }
      Log.v("BTLE", "Done wait for not working: " + writeStatus);
    }
  }

  @Override
  public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
    if (status == BluetoothGatt.GATT_SUCCESS) {
      onCharacteristicChanged(gatt, characteristic);
    }
  }

  public abstract void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic
      characteristic);
}
