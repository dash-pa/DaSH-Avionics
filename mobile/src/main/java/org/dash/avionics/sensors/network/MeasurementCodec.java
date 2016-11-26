package org.dash.avionics.sensors.network;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.dash.avionics.data.Measurement;
import org.dash.avionics.data.MeasurementType;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Logic for encoding and decoding measurements for network transmission.
 */
public class MeasurementCodec {
  private static final byte SERIALIZATION_VERSION = 1;

  // Main header
  private static final int SIZE_VERSION = 1;
  private static final int SIZE_COUNT = 1;  // Future use: batching.
  // Individual measurement.
  private static final int SIZE_TIMESTAMP = 8;
  private static final int SIZE_TYPE = 1;
  private static final int SIZE_VALUE = 4;
  // Footer
  private static final int SIZE_MD5 = 16;

  private static final int SIZE_FULL_MEASUREMENT = SIZE_TIMESTAMP + SIZE_TYPE + SIZE_VALUE;
  private static final int SIZE_OVERHEAD = SIZE_VERSION + SIZE_COUNT + SIZE_MD5;

  private static final int MAX_COUNT = 100;
  private static final int MIN_SIZE = SIZE_OVERHEAD + SIZE_FULL_MEASUREMENT;
  private static final int MAX_SIZE = SIZE_OVERHEAD + MAX_COUNT * SIZE_FULL_MEASUREMENT;

  public static byte[] serialize(List<Measurement> measurements) throws IOException {
    Preconditions.checkArgument(measurements.size() < MAX_COUNT,
        "Too many measurements to serialize: " + measurements.size());

    ByteBuffer buffer = ByteBuffer.allocate(MAX_SIZE);
    buffer.put(SERIALIZATION_VERSION)
        .put((byte) measurements.size());  // Count
    for (Measurement m : measurements) {
      buffer.putLong(m.timestamp)
          .putInt(m.type.ordinal())
          .putFloat(m.value);
    }

    int size = buffer.position();
    byte[] result = new byte[size + SIZE_MD5];
    buffer.rewind();
    buffer.get(result);

    appendMD5(result, size);
    return result;
  }

  public static List<Measurement> deserialize(byte[] buf) throws IOException {
    if (buf.length < MIN_SIZE) {
      throw new StreamCorruptedException("Bad datagram size: " + buf.length);
    }

    byte[] actualMD5 = new byte[SIZE_MD5];
    calculateMD5(buf, buf.length - SIZE_MD5, actualMD5, 0);

    ByteBuffer buffer = ByteBuffer.wrap(buf);
    byte version = buffer.get();
    if (version != SERIALIZATION_VERSION) {
      throw new UnsupportedEncodingException("Bad serialization version: " + Byte.toString(version));
    }
    byte count = buffer.get();
    if (count < 1 || buf.length != SIZE_OVERHEAD + count * SIZE_FULL_MEASUREMENT) {
      throw new UnsupportedEncodingException("Bad batch size: " + count + " for buffer size " +
          buf.length);
    }

    ArrayList<Measurement> result = Lists.newArrayListWithExpectedSize(count);
    for (int i = 0; i < count; i++) {
      long timestamp = buffer.getLong();
      int typeOrdinal = buffer.getInt();
      if (typeOrdinal >= MeasurementType.values().length) {
        throw new StreamCorruptedException("Bad measurement type: " + typeOrdinal);
      }
      MeasurementType type = MeasurementType.values()[typeOrdinal];
      float value = buffer.getFloat();

      result.add(new Measurement(type, value, timestamp));
    }

    byte[] reportedMD5 = new byte[SIZE_MD5];
    buffer.get(reportedMD5);
    if (!Arrays.equals(reportedMD5, actualMD5)) {
      throw new StreamCorruptedException("MD5s don't match.");
    }
    return result;
  }

  private static void appendMD5(byte[] contents, int length) throws IOException {
    calculateMD5(contents, length, contents, length);
  }

  private static void calculateMD5(byte[] inContents, int inLength, byte[] outContents, int
      outOffset) throws IOException {
    MessageDigest md5;
    try {
      md5 = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new UnsupportedEncodingException("Failed to calculate MD5: " + e);
    }
    Preconditions.checkState(md5.getDigestLength() == SIZE_MD5);
    Preconditions.checkArgument(outContents.length >= outOffset + md5.getDigestLength());
    md5.update(inContents, 0, inLength);
    try {
      md5.digest(outContents, outOffset, md5.getDigestLength());
    } catch (DigestException e) {
      throw new UnsupportedEncodingException("Failed to calculate MD5: " + e);
    }
  }
}
