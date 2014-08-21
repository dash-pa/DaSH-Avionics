package org.dash.avionics.alerts;

import java.util.EnumMap;

import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;

public class CruiseSpeedCalculator {
	public static final float HEAVY_PILOT_WEIGHT_KG = 90.0f;
	public static final float MEDIUM_PILOT_WEIGHT_KG = 72.6f;
	public static final float LIGHT_PILOT_WEIGHT_KG = 58.6f;

	public enum Aircraft {
		V5,
		V6,
		V6_EXTENDED_WINGS,
	}

	private static class LinearSpeedInterpolation {
		float a, b;

		LinearSpeedInterpolation(float lowWeight, float lowSpeed, float highWeight, float highSpeed) {
			this.a = (highSpeed-lowSpeed) / (highWeight-lowWeight);
			this.b = highSpeed - (this.a * highWeight);
		}

		float getSpeed(float weight) {
			return a * weight + b;
		}
	}

	private static RangeMap<Float, LinearSpeedInterpolation> buildInterpolationRanges(
			float heavySpeed, float mediumSpeed, float lightSpeed) {
		return ImmutableRangeMap.<Float, LinearSpeedInterpolation>builder()
				.put(Range.atMost(mediumSpeed), new LinearSpeedInterpolation(LIGHT_PILOT_WEIGHT_KG, lightSpeed, MEDIUM_PILOT_WEIGHT_KG, mediumSpeed))
				.put(Range.greaterThan(mediumSpeed), new LinearSpeedInterpolation(MEDIUM_PILOT_WEIGHT_KG, mediumSpeed, HEAVY_PILOT_WEIGHT_KG, heavySpeed))
				.build();

	}

	private static final EnumMap<Aircraft, RangeMap<Float, LinearSpeedInterpolation>> REFERENCE_SPEEDS;
	static {
		REFERENCE_SPEEDS = Maps.newEnumMap(Aircraft.class);
		REFERENCE_SPEEDS.put(Aircraft.V5, buildInterpolationRanges(24.6f, 22.9f, 21.3f));
		REFERENCE_SPEEDS.put(Aircraft.V6, buildInterpolationRanges(23.9f, 22.2f, 20.7f));
		REFERENCE_SPEEDS.put(Aircraft.V6_EXTENDED_WINGS, buildInterpolationRanges(23.4f, 21.8f, 20.3f));
	}

	public static float getCruiseAirspeed(Aircraft acft, float pilotWeightKg) {
		RangeMap<Float, LinearSpeedInterpolation> speedRanges = REFERENCE_SPEEDS.get(acft);
		LinearSpeedInterpolation speedRange = speedRanges.get(pilotWeightKg);
		return speedRange.getSpeed(pilotWeightKg);
	}
}
