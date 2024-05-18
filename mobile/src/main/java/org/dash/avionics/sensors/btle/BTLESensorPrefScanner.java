package org.dash.avionics.sensors.btle;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import org.dash.avionics.sensors.SensorListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Base class for BTLE sensor managers.
 */
public abstract class BTLESensorPrefScanner extends BluetoothGattCallback
        implements BluetoothAdapter.LeScanCallback {
  private enum WriteDescriptorStatus {
    FAILURE, DONE, WORKING
  }

  private static final UUID CLIENT_CHARACTERISTIC_CONFIG =
          UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

  private static final int MAX_SERVICE_RETRIES = 3;

  private final Context context;
  private final List<String> deviceNamePrefixes;
  private final List<UUID> requiredCharacteristics;
  private final BluetoothAdapter btadapter;
  private SensorListener listener = null;
  private List<BluetoothGatt> gatts = null;
  private WriteDescriptorStatus writeStatus = WriteDescriptorStatus.DONE;
  private final Object writeStatusLock = new Object();

  private List<String> scannedDevices = new ArrayList<String>();
  private Map<String, String> validDevices = new HashMap<String, String>();

  private UUID SERVICE_UUID = UUID.fromString("961f0001-d2d6-43e3-a417-3bb8217e0e01");
  private static final UUID CHARACTERISTIC = UUID.fromString("961f0005-d2d6-43e3-a417-3bb8217e0e01");

  /**
   *
   * @param context application context
   * @param deviceNamePrefixes list of allowed device name prefixes (all if empty)
   * @param requiredCharacteristics list of required BTLE characaristic UUIDs (any if empty)
   */
  public BTLESensorPrefScanner(Context context, List<String> deviceNamePrefixes, List<UUID> requiredCharacteristics) {
    this.context = context;
    this.deviceNamePrefixes = deviceNamePrefixes;
    this.requiredCharacteristics = requiredCharacteristics;
    btadapter = BluetoothAdapter.getDefaultAdapter();
    gatts = new ArrayList<>();
  }

  public void startScan(SensorListener listener) {
    if (context.checkSelfPermission("android.permission.BLUETOOTH_SCAN") == PackageManager.PERMISSION_GRANTED) {
      this.listener = listener;
      // TODO: Update for API 22.
      Log.d("BTLEPS", "Trying to start BTLE scan");
      if (btadapter == null) {
        Log.e("BTLEPS", "BT adapter is null");
        return;
      }
      if (btadapter.getBluetoothLeScanner() == null) {
        Log.e("BTLEPS", "BTLE scanner is null");
        return;
      }
      if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
        Log.d("BTLEPS", "Permissions are valid, starting scan");
        //noinspection deprecation
        btadapter.startLeScan(this);
      }
    } else {
      Log.e("BTLEPS", "Unable to start scan, permission not granted");
    }
  }

  @SuppressLint("MissingPermission")
  public void stopScan() {
    if (btadapter != null) btadapter.stopLeScan(this);
    this.disconnectAll();
  }


  protected SensorListener getListener() {
    return listener;
  }

  @SuppressLint("MissingPermission")
  private void disconnectAll() {
    Log.d("BTLEPS" ,"Disconnect All");
    gatts.forEach(g -> {
      if (g != null) {
        g.disconnect();
        g.close();
      }
    });
    gatts.clear();
    synchronized (writeStatusLock) {
      setWriteDescriptorStatus(WriteDescriptorStatus.DONE);
    }
  }

  @SuppressLint("MissingPermission")
  private void closeGatt(BluetoothGatt gatt) {
    Log.d("BTLEPS" ,"closeGatt");
    if (gatt != null) {
      gatt.disconnect();
      gatt.close();
      gatts.remove(gatt);
    }
  }

  @Override
  public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
      return;
    }
    String dAddress = device.getAddress();
    if (scannedDevices.contains(dAddress)) {
      Log.d("BTLEPS", "Skipping already scanned device: " + dAddress);
      return;
    }
    if (device == null || device.getName() == null) return;

    String name = device.getName();
    boolean validName = deviceNamePrefixes.isEmpty() || deviceNamePrefixes.stream().anyMatch(name::startsWith);
    if (!validName) return;

    Log.d("BTLEPS", "Device found:" + name + ":" + dAddress);

    validDevices.put(dAddress, name);

    boolean validCharacteristics = requiredCharacteristics.isEmpty();
    ParcelUuid[] pUUIDs = device.getUuids();
    if (pUUIDs != null && requiredCharacteristics != null) {
      List<UUID> deviceCharacteristics = Arrays.stream(device.getUuids()).map(p -> p.getUuid()).collect(Collectors.toList());
      Log.d("BTLEPS", deviceCharacteristics.toString());
      //noinspection SlowListContainsAll
      validCharacteristics = validCharacteristics && deviceCharacteristics.containsAll(requiredCharacteristics);
    }

    if (validCharacteristics) {
      // Device is valid, notify the listener of a new device
      gatts.add(device.connectGatt(context, true, this));
      scannedDevices.add(dAddress);
      Log.d("BTLEPS", "Adding device " + name + " -- " + dAddress);
    } else {
      Log.w("BTLEPS", "Device rejected by characteristics:" + device.getName() + ":" + device.getAddress());
    }
    // Wahoo == TICKR

//    Log.d("BTLEPS", "BTLE device: " + device.getName() + ",  Address: " + device.getAddress());
//    Log.d("BTLEPS", "Searching for device with prefix " + deviceNamePrefix);
//    if (!characteristicsEnabled && device.getName().startsWith(deviceNamePrefix) && onDeviceFound(device)) {
//      Log.d("BTLEPS", "Connected to device " + device.getName());
//      gatt = device.connectGatt(context, false, this); // set this as the gatt handler
//    }
  }

  // sets isConnected and discovers services when connected
  @SuppressLint("MissingPermission")
  @Override
  public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
    if (newState == BluetoothProfile.STATE_CONNECTED) {
        Log.i("BTLEPS", "BTGatt connected");
        gatt.discoverServices();
    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
      //Reconnect if we were disconnected but this is the correct device.
      scannedDevices.remove(gatt.getDevice().getAddress());
      closeGatt(gatt);
        Log.i("BTLEPS", "BTGatt disconnected");
    } else {
      Log.w("BTLEPS", "BTGatt state " + newState);
    }
  }

  // enables notification for the power and heart rate characteristics and sets isReady
  @SuppressLint("MissingPermission")
  @Override
  public void onServicesDiscovered(BluetoothGatt gatt, int status) {
    if (status != BluetoothGatt.GATT_SUCCESS) {
      Log.w("BTLEPS", "BTGatt failed to discover services!");
      return;
    }

    Log.d("BTLEPS", "BTGatt services discovered");
    List<BluetoothGattService> servicesList = gatt.getServices();
    if (servicesList == null) {
      Log.w("BTLEPS", "No services!");
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
    Log.v("BTLEPS", deviceStr.toString());

    String dName = gatt.getDevice().getName();
    if (dName == null)
      dName = gatt.getDevice().getAlias();
    if (dName != null)
     validDevices.put(gatt.getDevice().getAddress(), dName);

    // If the service we want is on this device, add it to the list of allowed devices if it's not already there.
    listener.onDeviceListChange(validDevices);

    enableCharacteristic(gatt, SERVICE_UUID, CHARACTERISTIC, "wind");
  }

  @SuppressLint("MissingPermission")
  protected void enableCharacteristic(BluetoothGatt gatt, UUID serviceUuid, UUID characteristicUuid, String
          serviceName) {
    BluetoothGattService service = gatt.getService(serviceUuid);
    int tries = 0;
    while (service == null && tries < MAX_SERVICE_RETRIES) {
      tries++;
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Log.w("BTLE", "Interrupted while sleeping for service " + serviceName);
        return;
      }
      service = gatt.getService(serviceUuid);
    }

    if (service == null) {
      Log.w("BTLE", serviceName + " service is null!");
      return;
    }

    Log.w("BTLE", serviceName + " service found on try #" + (tries + 1));

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

  @Override
  public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
    if (status == BluetoothGatt.GATT_SUCCESS) {
      onCharacteristicChanged(gatt, characteristic);
    }
  }

  public abstract void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic
      characteristic);
}
