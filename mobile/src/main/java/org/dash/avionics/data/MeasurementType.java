package org.dash.avionics.data;

public enum MeasurementType {
  CRANK_RPM, IMPELLER_RPM, POWER, HEART_BEAT, HEADING, SPEED, HEIGHT,
  GPS_LATITUDE, GPS_LONGITUDE, GPS_ALTITUDE, GPS_SPEED, GPS_BEARING,
  // Really the same as speed, but from a different sensor.
  AIRSPEED,
}