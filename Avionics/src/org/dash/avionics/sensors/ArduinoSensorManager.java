package org.dash.avionics.sensors;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class ArduinoSensorManager {
	private InputStream arduinoOutput;
	private BluetoothSocket socket;

	public void connect() throws IOException {
		if (socket != null) {
			if (socket.isConnected()) {
				return;
			}

			// Connection is stale, try to reconnect.
			Log.w("Arduino", "Trying to reconnect over bluetooth");
			disconnect();
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

	public void disconnect() {
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
			if (device.getName().equals("HC-06")) {
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

	/**
	 * Reads an update from the Arduino over bluetooth, and returns its parsed version.
	 */
	public ValueUpdate readUpdate() throws IOException {
		ValueUpdate update = null;
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
			} else if (chr == '\n') {
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

	private ValueUpdate parseUpdate(String line) {
		int splitPos = line.indexOf(':');
		if (splitPos == -1 || line.length() < splitPos + 1) {
			Log.e("Arduino", "Malformed line '" + line + "'.");
			return null;
		}

		String typeStr = line.substring(0, splitPos);
		ValueType type = parseLineType(typeStr);
		if (type == null) {
			return null;
		}

		String valueStr = line.substring(splitPos + 1);
		int value = parseLineValue(valueStr);

		return new ValueUpdate(type, value);
	}

	private ValueType parseLineType(String typeStr) {
		if (typeStr.equals("RPM")) {
			return ValueType.RPM;
		} else if (typeStr.equals("ALT")) {
			return ValueType.HEIGHT;
		} else if (typeStr.equals("KPH")) {
			return ValueType.SPEED;
		}
		return null;
	}

	private int parseLineValue(String valueStr) {
		return (int) Double.parseDouble(valueStr);
	}
}
