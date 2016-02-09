package org.dash.avionics.sensors.weathermeter;

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

import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT16;

/**
 * Manager for the WeatherMeter sensor.
 */
@EBean
public class WeatherMeterSensorManager extends BluetoothGattCallback
    implements SensorManager, BluetoothAdapter.LeScanCallback {
  private enum WriteDescriptorStatus {
    FAILURE, DONE, WORKING
  }

  private static final UUID CLIENT_CHARACTERISTIC_CONFIG =
      UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

  private static final UUID WIND_SERVICE_UUID =
      UUID.fromString("961f0001-d2d6-43e3-a417-3bb8217e0e01");
  private static final UUID ENVIRO_CHARACTERISTIC =
      UUID.fromString("961f0005-d2d6-43e3-a417-3bb8217e0e01");

  private final Context context;
  private final BluetoothAdapter btadapter;
  private SensorListener listener = null;
  private BluetoothGatt gatt = null;
  private WriteDescriptorStatus writeStatus = WriteDescriptorStatus.DONE;
  private final Object writeStatusLock = new Object();
  private boolean characteristicsEnabled;

  public WeatherMeterSensorManager(Context context) {
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
    Log.d("WeatherMeter", "Disconnecting");
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
    Log.d("WeatherMeter", "Starting BTLE scan");
    //noinspection deprecation
    btadapter.startLeScan(this);
  }

  @Override
  public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
    if (device == null || device.getName() == null) {
      return;
    }

    Log.d("WeatherMeter", "BTLE device: " + device.getName() + " " + device.getAddress());
    if (device.getName().startsWith("WFANO")) {
      //noinspection deprecation
      btadapter.stopLeScan(this);
      gatt = device.connectGatt(context, false, this); // set this as the gatt handler
    }
  }

  // sets isConnected and discovers services when connected
  @Override
  public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
    if (newState == BluetoothProfile.STATE_CONNECTED) {
      Log.i("WeatherMeter", "BTGatt connected");
      gatt.discoverServices();
    } else {
      Log.w("WeatherMeter", "BTGatt state " + newState);
    }
  }

  // enables notification for the power and heart rate characteristics and sets isReady
  @Override
  public void onServicesDiscovered(BluetoothGatt gatt, int status) {
    if (status != BluetoothGatt.GATT_SUCCESS) {
      Log.w("WeatherMeter", "BTGatt failed to discover services!");
      return;
    }

    Log.d("WeatherMeter", "BTGatt services discovered");
    List<BluetoothGattService> servicesList = gatt.getServices();
    if (servicesList == null) {
      Log.w("WeatherMeter", "No services!");
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
    Log.v("WeatherMeter", deviceStr.toString());

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
    enableCharacteristic(WIND_SERVICE_UUID, ENVIRO_CHARACTERISTIC, "wind");
  }

  private void enableCharacteristic(UUID serviceUuid, UUID characteristicUuid, String serviceName) {
    BluetoothGattService service = gatt.getService(serviceUuid);
    if (service == null) {
      Log.w("WeatherMeter", serviceName + " service is null!");
      return;
    }

    BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
    if (characteristic == null) {
      Log.w("WeatherMeter", serviceName + " characteristic is null!");
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

      Log.w("WeatherMeter", errStr.toString());
      return;
    }

    // Set Enable or Disable notification on the descriptor depending on argument val
    Log.v("WeatherMeter", "Enabling notifications for " + descriptor.getUuid());
    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

    // Wait for any possible descriptor writing to finish
    waitForWriteDescriptorNotWorking();
    setWriteDescriptorStatus(WriteDescriptorStatus.WORKING);

    if (!gatt.writeDescriptor(descriptor)) { // failed to initiate descriptor write
      // We have to free this up for future calls to setNotifyValue to work
      setWriteDescriptorStatus(WriteDescriptorStatus.DONE);
      Log.w("WeatherMeter", serviceName + " descriptor write failed.");
      return;
    }

    // Wait for the descriptor to be written
    waitForWriteDescriptorNotWorking();

    // If there was a failure, this would be equal to WD_STATUS_FAILURE instead of WD_STATUS_DONE
    if (writeStatus != WriteDescriptorStatus.DONE) {
      // Done writing descriptors
      setWriteDescriptorStatus(WriteDescriptorStatus.DONE);
      Log.w("WeatherMeter", serviceName + " descriptor write failed with status " + writeStatus);
      return;
    }

    Log.i("WeatherMeter", "enabled " + serviceName + " notifications");
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
      Log.v("WeatherMeter", "Starting wait for not working: " + writeStatus);
      while (writeStatus == WriteDescriptorStatus.WORKING) {
        try {
          writeStatusLock.wait();
        } catch (InterruptedException e) {
          // Try again.
        }
      }
      Log.v("WeatherMeter", "Done wait for not working: " + writeStatus);
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
    if (characteristic.getUuid().equals(ENVIRO_CHARACTERISTIC)) {
      handleWindData(characteristic);
    }
  }

  private void handleWindData(BluetoothGattCharacteristic characteristic) {
    int flag = characteristic.getProperties();
    int rpm = characteristic.getIntValue(FORMAT_UINT16, 0);
    float speed = rpms2kph(rpm);

//    Log.v("WeatherMeter", "New wind notification: rpm=" + rpm + "; speed=" + speed +
//        "values=" + Arrays.toString(characteristic.getValue()));

    listener.onNewMeasurement(new Measurement(MeasurementType.AIRSPEED, speed));
  }

  /** Convert RPM to km/h. */
  private float rpms2kph(int rpm) {
    if (rpm < 60) return 0.0f;
    return 0.003693942f * rpm + 2.90074752f;
  }
}
