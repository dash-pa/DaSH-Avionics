package org.dash.avionics.sensors.network;

import android.util.Log;

import com.google.common.collect.Lists;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.dash.avionics.data.Measurement;
import org.dash.avionics.sensors.SensorListener;
import org.dash.avionics.sensors.SensorPreferences_;

import java.io.IOException;

/**
 * Sensor listener which sends measurements over the network.
 */
@EBean
public class UDPMeasurementSender implements SensorListener {
  private UDPSocketHelper socket;

  @Pref
  SensorPreferences_ preferences;

  @Override
  public void onNewMeasurement(Measurement measurement) {
    if (!preferences.isUdpSendingEnabled().get()) {
      if (socket != null) {
        socket.close();
        socket = null;
      }
      return;
    }
    if (socket == null) {
      socket = new UDPSocketHelper(NetworkConstants.UDP_RECEIVE_PORT);
    }

    String address = preferences.getUdpSendingAddress().get();
    try {
      byte[] encoded = MeasurementCodec.serialize(Lists.newArrayList(measurement));
      socket.send(encoded, address);
    } catch (IOException e) {
      Log.e("Dash.UDP", "Failed to send packet", e);
    }
  }
}
