package org.dash.avionics.sensors.network;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.dash.avionics.data.Measurement;
import org.dash.avionics.data.MeasurementType;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
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
  private static final int SIZE_FULL_MEASUREMENT = SIZE_TIMESTAMP + SIZE_TYPE + SIZE_VALUE;
  // Footer
  private static final int MD5_SIZE = 16;

  private static final int MAX_COUNT = 100;
  private static final int MIN_SIZE = SIZE_VERSION+SIZE_COUNT+SIZE_FULL_MEASUREMENT+MD5_SIZE;
  private static final int MAX_SIZE = SIZE_VERSION+SIZE_COUNT + MAX_COUNT*SIZE_FULL_MEASUREMENT
      + MD5_SIZE;

  public static byte[] serialize(List<Measurement> measurements) {
    ByteBuffer buffer = ByteBuffer.allocate(MAX_SIZE);
    buffer.put(SERIALIZATION_VERSION);
    Preconditions.checkArgument(measurements.size() < MAX_COUNT,
        "Too many measurements to serialize: " + measurements.size());
    buffer.put((byte) measurements.size());  // Count
    for (Measurement m : measurements) {
      buffer.putLong(m.timestamp);
      buffer.putInt(m.type.ordinal());
      buffer.putFloat(m.value);
    }
    // TODO: Output proper MD5
    buffer.put(new byte[16]);
    return Arrays.copyOf(buffer.array(), buffer.position());
  }

  public static List<Measurement> deserialize(byte[] buf) throws IOException {
    if (buf.length < MIN_SIZE) {
      throw new StreamCorruptedException("Bad datagram size: " + buf.length);
    }

    ByteBuffer buffer = ByteBuffer.wrap(buf);
    byte version = buffer.get();
    if (version != SERIALIZATION_VERSION) {
      throw new UnsupportedEncodingException("Bad serialization version: " + Byte.toString(version));
    }
    byte count = buffer.get();
    if (count < 1) {
      throw new UnsupportedEncodingException("Bad batch size: " + Byte.toString(count));
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
    // TODO: Validate MD5
    return result;
  }
}
