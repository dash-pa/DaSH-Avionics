package org.dash.avionics.data.model;

/**
 * Value model which can be explicitly set.
 */
public class SettableValueModel<T> extends AbstractValueModel implements ValueModel<T> {

  private T value;

  public SettableValueModel() {}

  public SettableValueModel(SettableValueModel<T> other) {
    super(other);

    this.value = other.value;
  }

  public void setValue(T value) {
    this.value = value;
    setValueTime(now());
  }

  @Override
  public T getValue() {
    return value;
  }

  @Override
  public boolean isValid() {
    return value != null;
  }

  @Override
  protected Object clone() {
    return new SettableValueModel<T>(this);
  }

}
