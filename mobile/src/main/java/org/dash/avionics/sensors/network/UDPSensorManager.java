package org.dash.avionics.sensors.network;

import android.util.Log;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.api.BackgroundExecutor;
import org.dash.avionics.data.Measurement;
import org.dash.avionics.sensors.SensorListener;
import org.dash.avionics.sensors.SensorManager;

import java.io.IOException;
import java.util.List;

/**
 * {@link SensorManager} which reports values received over the network.
 */
@EBean
public class UDPSensorManager implements SensorManager {

  private SensorListener updater;
  private UDPSocketHelper socket;

  @Background(serial = "receive")
  @Override
  public void connect(SensorListener updater) {
    this.updater = updater;
    this.socket = new UDPSocketHelper(NetworkConstants.UDP_RECEIVE_PORT);

    while (socket != null) {
      receiveUdpOnce();
    }
  }

  private void receiveUdpOnce() {
    UDPSocketHelper lastSocket = socket;
    if (socket == null) {
      return;
    }

    byte[] buffer;
    try {
      buffer = lastSocket.receive();
    } catch (IOException e) {
      Log.w("Dash.UDP", "Unable to receive datagram", e);
      return;
    }

    List<Measurement> measurements;
    try {
      measurements = MeasurementCodec.deserialize(buffer);
    } catch (IOException e) {
      Log.w("Dash.UDP", "Unable to decode datagram", e);
      return;
    }

    for (Measurement m: measurements) {
      updater.onNewMeasurement(m);
    }
  }

  @Override
  public void disconnect() {
    BackgroundExecutor.cancelAll("receive", true);
    if (socket != null) {
      socket.close();
      socket = null;
    }
  }
}
