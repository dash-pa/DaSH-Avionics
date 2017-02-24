package org.dash.avionics.sensors.network;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * Helper for setting up UDP sockets and dealing with datagrams.
 */
public class UDPSocketHelper {
  private final int port;
  private DatagramSocket socket;
  private String lastPeer;
  private InetAddress lastPeerAddress;

  public UDPSocketHelper(int port) {
    this.port = port;
  }

  private void tryOpenSocket(int localPort) throws IOException {
    if (socket == null || !socket.isBound() || socket.isClosed() ||
        (localPort != 0 && socket.getLocalPort() != localPort)) {
      Log.i("Dash.UDP", "(re)creating UDP socket on port " + localPort);
      if (socket != null) {
        Log.d("Dash.UDP", "Old socket: bound=" + socket.isBound() + "; closed=" + socket.isClosed
            () + "; port=" + socket.getLocalPort());
      }
      socket = new DatagramSocket(localPort);
    }
    if (!socket.isBound()) {
      throw new IOException("Socket not bound");
    }
    if (socket.isClosed()) {
      throw new IOException("Socket closed");
    }
  }

  public void send(byte[] data, String destination) throws IOException {
    if (!destination.equals(lastPeer)) {
      lastPeer = destination;
      lastPeerAddress = InetAddress.getByName(destination);
    }

    DatagramPacket packet =
        new DatagramPacket(data, data.length, lastPeerAddress, port);
    DatagramSocket lastSocket;
    synchronized (this) {
      tryOpenSocket(0);  // Send *from* any port.
      lastSocket = socket;
    }
    lastSocket.send(packet);
//    Log.v("Dash.UDP", "Sent packet to " + lastPeerAddress + ":" + port);
  }

  public byte[] receive() throws IOException {
    byte[] buffer = new byte[NetworkConstants.BUFFER_SIZE];
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    DatagramSocket lastSocket;
    synchronized (this) {
      tryOpenSocket(port);
      lastSocket = socket;
    }
    lastSocket.receive(packet);

//    Log.v("Dash.UDP", "Received packet from " + packet.getSocketAddress());
    return Arrays.copyOf(packet.getData(), packet.getLength());
  }

  /**
   * Closes the socket (but any send or receive operation will re-open it).
   */
  public void close() {
    synchronized (this) {
      if (socket != null) {
        socket.close();
      }
      socket = null;
    }
  }
}
