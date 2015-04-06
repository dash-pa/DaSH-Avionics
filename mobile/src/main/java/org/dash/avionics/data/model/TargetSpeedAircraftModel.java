package org.dash.avionics.data.model;

public class TargetSpeedAircraftModel implements AircraftModel {
  private final float targetSpeed;
  private final float speedMargin;

  public TargetSpeedAircraftModel(float targetSpeed, float speedMargin) {
    this.targetSpeed = targetSpeed;
    this.speedMargin = speedMargin;
  }

  @Override
  public float getVs0() {
    return targetSpeed - speedMargin * 2;
  }

  @Override
  public float getVs1() {
    return targetSpeed - speedMargin;
  }

  @Override
  public float getVno() {
    return targetSpeed + speedMargin;
  }

  @Override
  public float getVne() {
    return targetSpeed + speedMargin * 2;
  }
}
