package org.dash.avionics.display.model;

/**
 * Created by rdamazio on 3/15/15.
 */
public class RecentSettableValueModel<T> extends SettableValueModel<T> {

  private long lastValueUpdate;
  private final long maxAge;

  public RecentSettableValueModel(long maxAge) {
    this.maxAge = maxAge;
  }

  public RecentSettableValueModel(RecentSettableValueModel<T> other) {
    this.maxAge = other.maxAge;
    this.setValue(other.getValue());
    this.lastValueUpdate = other.lastValueUpdate;
  }

  @Override
  public void setValue(T value) {
    super.setValue(value);

    lastValueUpdate = now();
  }

  @Override
  public boolean isValid() {
    return super.isValid() && now() - lastValueUpdate < maxAge;
  }

  @Override
  public Object clone() {
    return new RecentSettableValueModel<T>(this);
  }

  public long getLastValueUpdate() {
    return lastValueUpdate;
  }
}
