package org.dash.avionics.aircraft;

import junit.framework.TestCase;

public class CruiseSpeedCalculatorTest extends TestCase {
  public void testGetCruiseAirspeed() throws Exception {
    //Actual values from table
    assertEquals(23.04f, CruiseSpeedCalculator.getCruiseAirspeed(AircraftType.V5, 50.8f));
    assertEquals(23.7f, CruiseSpeedCalculator.getCruiseAirspeed(AircraftType.V5, 56.70f));
    assertEquals(27.13f, CruiseSpeedCalculator.getCruiseAirspeed(AircraftType.V5, 90.00f));

    //Interpolation between two values
    assertEquals(23.292513f, CruiseSpeedCalculator.getCruiseAirspeed(AircraftType.V5, 53.0f));
    assertEquals(23.62291f, CruiseSpeedCalculator.getCruiseAirspeed(AircraftType.V5, 56.0f));
    //Between the top and top-1 as this is a special case in CruiseSpeedCalculator
    assertEquals(26.6456f, CruiseSpeedCalculator.getCruiseAirspeed(AircraftType.V5, 85f));

    //Interpolation below min weight
    assertEquals(22.391188f, CruiseSpeedCalculator.getCruiseAirspeed(AircraftType.V5, 45.0f));

    //Interpolation above max weight
    assertEquals(27.614399f, CruiseSpeedCalculator.getCruiseAirspeed(AircraftType.V5, 95.0f));

    //Switching aircraft type
    assertEquals(22.17f, CruiseSpeedCalculator.getCruiseAirspeed(AircraftType.V6_EXTENDED_WINGS, 56.70f));
  }

}