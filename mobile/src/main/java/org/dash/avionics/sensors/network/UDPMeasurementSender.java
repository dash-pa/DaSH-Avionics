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
  private final UDPSocketHelper socket;

  @Pref
  SensorPreferences_ preferences;

  public UDPMeasurementSender() {
    socket = new UDPSocketHelper(0);  // Send *from* any port.
  }

  @Override
  public void onNewMeasurement(Measurement measurement) {
    if (!preferences.isUdpSendingEnabled().get()) {
      return;
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
