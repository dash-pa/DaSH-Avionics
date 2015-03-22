package org.dash.avionics.display.model;

public interface ValueModel <T> extends Cloneable {
  T getValue();
  long getValueTime();
  boolean isValid();
}
