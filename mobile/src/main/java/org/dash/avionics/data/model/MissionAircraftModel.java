package org.dash.avionics.data.model;

public class MissionAircraftModel implements AircraftModel {
  private final float targetSpeed;
  private final float speedMargin;
  private final float targetHeight;
  private final float heightMargin;

  public MissionAircraftModel(float targetSpeed, float speedMargin,
                              float targetHeight, float heightMargin) {
    this.targetSpeed = targetSpeed;
    this.speedMargin = speedMargin;
    this.targetHeight = targetHeight;
    this.heightMargin = heightMargin;
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

  @Override
  public float getMinHeight() {
    return targetHeight - heightMargin * 2;
  }

  @Override
  public float getLowHeight() {
    return targetHeight - heightMargin;
  }

  @Override
  public float getHighHeight() {
    return targetHeight + heightMargin;
  }

  @Override
  public float getMaxHeight() {
    return targetHeight + heightMargin * 2;
  }
}
