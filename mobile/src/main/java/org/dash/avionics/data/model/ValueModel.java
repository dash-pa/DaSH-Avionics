package org.dash.avionics.data.model;

public interface ValueModel <T> extends Cloneable {
  T getValue();
  long getValueTime();
  boolean isValid();
}
