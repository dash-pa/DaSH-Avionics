package org.dash.avionics.aircraft;

import android.util.Log;

import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;

import java.util.Map;

public class CruiseSpeedCalculator {
  public static final float HEAVY_PILOT_WEIGHT_KG = 90.0f;
  public static final float MEDIUM_PILOT_WEIGHT_KG = 72.6f;
  public static final float LIGHT_PILOT_WEIGHT_KG = 58.6f;

  public static final float[] pilotWeights = new float[]
          {50.8f, 56.7f, 61.24f, 65.77f, 74.84f, 83.91f, 90.0f};

  private CruiseSpeedCalculator() { }

  private static class LinearSpeedInterpolation {
    float a, b;

    LinearSpeedInterpolation(float lowWeight, float lowSpeed, float highWeight, float highSpeed) {
      this.a = (highSpeed - lowSpeed) / (highWeight - lowWeight);
      this.b = highSpeed - (this.a * highWeight);
    }

    float getSpeed(float weight) {
      return a * weight + b;
    }
  }

  private static RangeMap<Float, LinearSpeedInterpolation> buildInterpolationRanges(float[] speeds) {
    ImmutableRangeMap.Builder builder = ImmutableRangeMap.<Float, LinearSpeedInterpolation>builder();
    if (speeds.length > 1) {
      for (int i = 0; i < speeds.length - 1; i++) {
        Range r;
        if (i == 0) {
          r = Range.atMost(pilotWeights[i]);
        } else if (i == speeds.length - 2) {
          r = Range.greaterThan(pilotWeights[i-1]);
        } else {
          r = Range.openClosed(pilotWeights[i-1], pilotWeights[i]);
        }
        builder.put(r, new LinearSpeedInterpolation(pilotWeights[i], speeds[i], pilotWeights[i + 1], speeds[i + 1]));
      }
    } else {
      Log.e("Cruise Speed Calculator", "Unable to calculate a cruise speed range with less than two speeds");
    }

    return builder.build();
  }

  private static final Map<AircraftType, RangeMap<Float, LinearSpeedInterpolation>> REFERENCE_SPEEDS;

  static {
    REFERENCE_SPEEDS = Maps.newEnumMap(AircraftType.class);
    REFERENCE_SPEEDS.put(AircraftType.V5, buildInterpolationRanges(
            new float[]{22.37f, 23.05f, 23.56f, 24.06f, 25.02f, 25.96f, 26.56f}
    ));
    REFERENCE_SPEEDS.put(AircraftType.V6, buildInterpolationRanges(
            new float[]{21.84f, 22.52f, 23.01f, 23.5f, 24.44f, 25.34f, 25.93f}
    ));
    REFERENCE_SPEEDS.put(AircraftType.V6_EXTENDED_WINGS, buildInterpolationRanges(
            new float[]{21.36f, 22f, 22.48f, 22.95f, 23.86f, 24.73f, 25.3f}
    ));
  }

  public static float getCruiseAirspeed(AircraftType acft, float pilotWeightKg) {
    RangeMap<Float, LinearSpeedInterpolation> speedRanges = REFERENCE_SPEEDS.get(acft);
    LinearSpeedInterpolation speedRange = speedRanges.get(pilotWeightKg);
    if (speedRange != null) {
      return speedRange.getSpeed(pilotWeightKg);
    } else {
      return Float.NaN;
    }
  }

  public static float getCruiseAirspeedFromSettings(AircraftSettings_ settings) {
    AircraftType aircraftType = AircraftType.valueOf(settings.getAircraftType().get());
    float pilotWeightKg = settings.getPilotWeight().get();
    return getCruiseAirspeed(aircraftType, pilotWeightKg);
  }
}
