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
    //Pilot weight indexes (kg)
    //{50.8, 56.7, 61.24, 65.77, 74.84, 83.91, 90.0};
    REFERENCE_SPEEDS = Maps.newEnumMap(AircraftType.class);
    //33.3m wing
    REFERENCE_SPEEDS.put(AircraftType.V5, buildInterpolationRanges(
            new float[]{23.04f, 23.70f, 24.2f, 24.68f, 25.63f, 26.54f, 27.13f}
    ));
    //36.3m Wing (V5 WE)
    REFERENCE_SPEEDS.put(AircraftType.V5_EXTENDED_WINGS, buildInterpolationRanges(
            new float[]{22.51f, 23.15f, 23.63f, 24.09f, 25.01f, 25.9f, 26.47f}
    ));
    //40.3m wing V6 WE / V5 LWE
    REFERENCE_SPEEDS.put(AircraftType.V6_EXTENDED_WINGS, buildInterpolationRanges(
            new float[]{21.56f, 22.17f, 22.62f, 23.06f, 23.94f, 24.77f, 25.32f}
    ));
    //44m wing, V6 LWE
    REFERENCE_SPEEDS.put(AircraftType.V6_LONG_EXTENDED_WINGS, buildInterpolationRanges(
            new float[]{21.62f, 22.22f, 22.67f, 23.11f, 23.98f, 24.82f, 25.36f}
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
