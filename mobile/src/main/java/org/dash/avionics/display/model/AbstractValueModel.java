package org.dash.avionics.display.model;

/**
 * Created by rdamazio on 3/15/15.
 */
public class AbstractValueModel {
  private long valueTime;

  public AbstractValueModel() {}
  public AbstractValueModel(AbstractValueModel other) {
    this.valueTime = other.valueTime;
  }

  protected void setValueTime(long valueTime) {
    this.valueTime = valueTime;
  }

  public long getValueTime() {
    return valueTime;
  }

  protected long now() {
    return System.currentTimeMillis();
  }
}
