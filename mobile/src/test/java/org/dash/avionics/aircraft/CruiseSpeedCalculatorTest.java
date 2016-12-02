package org.dash.avionics.aircraft;

import junit.framework.TestCase;

public class CruiseSpeedCalculatorTest extends TestCase {
  public void testGetCruiseAirspeed() throws Exception {
    //Actual values from table
    assertEquals(22.37f, CruiseSpeedCalculator.getCruiseAirspeed(AircraftType.V5, 50.8f));
    assertEquals(23.05f, CruiseSpeedCalculator.getCruiseAirspeed(AircraftType.V5, 56.70f));
    assertEquals(26.56f, CruiseSpeedCalculator.getCruiseAirspeed(AircraftType.V5, 90.00f));

    //Interpolation between two values
    assertEquals(22.634361f, CruiseSpeedCalculator.getCruiseAirspeed(AircraftType.V5, 53.0f));
    assertEquals(22.971365f, CruiseSpeedCalculator.getCruiseAirspeed(AircraftType.V5, 56.0f));
    //Between the top and top-1 as this is a special case in CruiseSpeedCalculator
    assertEquals(26.067387f, CruiseSpeedCalculator.getCruiseAirspeed(AircraftType.V5, 85f));

    //Interpolation below min weight
    assertEquals(21.701527f, CruiseSpeedCalculator.getCruiseAirspeed(AircraftType.V5, 45.0f));

    //Interpolation above max weight
    assertEquals(27.05261f, CruiseSpeedCalculator.getCruiseAirspeed(AircraftType.V5, 95.0f));

    //Switching aircraft type
    assertEquals(22.52f, CruiseSpeedCalculator.getCruiseAirspeed(AircraftType.V6, 56.70f));
  }

}