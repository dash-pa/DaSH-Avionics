package org.dash.avionics.sensors.arduino;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.api.BackgroundExecutor;
import org.dash.avionics.calibration.CalibrationManager;
import org.dash.avionics.data.Measurement;
import org.dash.avionics.data.MeasurementType;
import org.dash.avionics.sensors.SensorListener;
import org.dash.avionics.sensors.SensorManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@EBean
public class ArduinoSensorManager implements SensorManager {
  private InputStream arduinoOutput;
  private BluetoothSocket socket;

  @Bean protected CalibrationManager calibrationManager;

  @Background(serial = "arduino-loop")
  @Override
  public void connect(SensorListener updater) {
    // Keep trying to connect and read from the sensors.
    while (true) {
      try {
        connectToDevice();
      } catch (IOException e) {
        Log.e("Arduino", "Failed to initialize", e);
        disconnectFromDevice();
        try {
          //noinspection BusyWait
          Thread.sleep(500);
        } catch (InterruptedException ie) {
          // Do nothing.
        }
        continue;
      }

      Measurement update;
      try {
        update = readUpdate();
      } catch (IOException e) {
        Log.w("Sensors", "Failed to read from Arduino", e);
        disconnectFromDevice();
        continue;
      }

      updater.onNewMeasurement(update);
    }
  }

  private void connectToDevice() throws IOException {
    if (socket != null) {
      if (socket.isConnected()) {
        return;
      }

      // Connection is stale, try to reconnect.
      Log.w("Arduino", "Trying to reconnect over bluetooth");
      disconnectFromDevice();
    }

    Log.i("Arduino", "Connecting");
    BluetoothDevice arduinoDevice = findArduinoDevice();
    if (arduinoDevice != null) {
      openSocket(arduinoDevice);
      Log.i("Arduino", "Connected");
    } else {
      throw new IOException("Arduino device not found");
    }
  }

  @Override
  public void disconnect() {
    Log.i("Arduino", "Disconnecting");
    BackgroundExecutor.cancelAll("arduino-loop", true);
    disconnectFromDevice();
  }

  private void disconnectFromDevice() {
    if (socket == null || arduinoOutput == null) {
      Log.w("Arduino", "Trying to disconnect already-disconnected socket");
      return;
    }

    try {
      arduinoOutput.close();
    } catch (IOException e) {
      Log.w("Arduino", "Failed to close bluetooth stream", e);
    }

    try {
      socket.close();
    } catch (IOException e) {
      Log.w("Arduino", "Failed to close bluetooth socket", e);
    }

    arduinoOutput = null;
    socket = null;
  }

  private BluetoothDevice findArduinoDevice() {
    // Get the HC-06 BlueTooth Transceiver device object
    BluetoothAdapter BTAdapter = BluetoothAdapter.getDefaultAdapter();
    Set<BluetoothDevice> pairedDevices = BTAdapter.getBondedDevices();
    for (BluetoothDevice device : pairedDevices) {
      if ("HC-06".equals(device.getName())) {
        return device;
      }
    }
    return null;
  }

  private void openSocket(BluetoothDevice arduinoDevice) throws IOException {
    // Standard SerialPortService ID.
    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    socket = arduinoDevice.createRfcommSocketToServiceRecord(uuid);
    socket.connect();
    arduinoOutput = socket.getInputStream();
  }

  private Measurement readUpdate() throws IOException {
    Measurement update = null;
    while (update == null) {
      String updateStr = readLine();
      update = parseUpdate(updateStr);
    }
    Log.v("Arduino", "Received update from Arduino: " + update);
    return update;
  }

  // Reads (blocking) from the socket up until a newline, then returns
  // everything before the newline.
  private String readLine() throws IOException {
    StringBuilder builder = new StringBuilder(32);
    while (true) {
      char chr = (char) arduinoOutput.read();
      if (chr == '\r') {
        continue;
      }
      if (chr == '\n') {
        break;
      }

      builder.append(chr);

      if (builder.length() >= 1024) {
        Log.e("Arduino", "Probable garbage being received, bailing: '"
            + builder + "'.");
        return builder.toString();
      }
    }
    return builder.toString();
  }

  private Measurement parseUpdate(String line) {
    int splitPos = line.indexOf(':');
    if (splitPos == -1 || line.length() < splitPos + 1) {
      Log.e("Arduino", "Malformed line '" + line + "'.");
      return null;
    }

    String typeStr = line.substring(0, splitPos);
    MeasurementType type = parseLineType(typeStr);
    if (type == null) {
      return null;
    }

    String valueStr = line.substring(splitPos + 1);
    float value = parseLineValue(valueStr);

    if (MeasurementType.IMPELLER_SPEED == type) {
      value = getCalibratedSpeed(value);
    }

    return new Measurement(type, value);
  }

  private float getCalibratedSpeed(float impellerRpm) {
    return impellerRpm * calibrationManager.loadActiveProfile().getImpellerRatio();
  }

  private MeasurementType parseLineType(String typeStr) {
    if ("RPM".equals(typeStr)) {
      return MeasurementType.IMPELLER_SPEED;
    }
    if ("ALT".equals(typeStr)) {
      return MeasurementType.HEIGHT;
    }
    return null;
  }

  private float parseLineValue(String valueStr) {
    return Float.parseFloat(valueStr);
  }
}
