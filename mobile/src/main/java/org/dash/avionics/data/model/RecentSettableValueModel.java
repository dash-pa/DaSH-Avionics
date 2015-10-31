package org.dash.avionics.data.model;

public class RecentSettableValueModel<T> extends SettableValueModel<T> {

  private final long maxAge;

  public RecentSettableValueModel(long maxAge) {
    this.maxAge = maxAge;
  }

  public RecentSettableValueModel(RecentSettableValueModel<T> other) {
    super(other);

    this.maxAge = other.maxAge;
  }

  @Override
  public boolean isValid() {
    return super.isValid() && now() - getValueTime() < maxAge;
  }

  @Override
  public Object clone() {
    return new RecentSettableValueModel<T>(this);
  }
}
