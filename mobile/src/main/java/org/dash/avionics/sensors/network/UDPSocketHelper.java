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
  private final int localPort;
  private final int peerPort;
  private DatagramSocket socket;
  private String lastPeer;
  private InetAddress lastPeerAddress;

  public UDPSocketHelper(int localPort, int peerPort) {
    this.localPort = localPort;
    this.peerPort = peerPort;

    try {
      tryOpenSocket();
    } catch (IOException e) {
      Log.w("UDPSocketHelper", "Failed to open socket - will retry", e);
    }
  }

  private void tryOpenSocket() throws IOException {
    if (socket == null || !socket.isBound() || socket.isClosed()) {
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
        new DatagramPacket(data, data.length, lastPeerAddress, peerPort);
    synchronized (this) {
      tryOpenSocket();
      socket.send(packet);
    }
//    Log.v("Dash.UDP", "Sent packet to " + lastPeerAddress + ":" + peerPort);
  }

  public byte[] receive() throws IOException {
    byte[] buffer = new byte[NetworkConstants.BUFFER_SIZE];
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    synchronized (this) {
      tryOpenSocket();
      socket.receive(packet);
    }
    if (packet.getPort() != peerPort) {
      Log.i("Dash.UDP", "Received datagram from unexpected port: " + packet.getPort());
    }
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
