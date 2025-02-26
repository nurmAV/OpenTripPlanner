package org.opentripplanner.routing.algorithm.transferoptimization.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.stoparrival.BasicPathTestCase;

class OptimizedPathTest implements RaptorTestConstants {

  @Test
  void copyBasicPath() {
    // Given: a wrapped basic path
    var path = new OptimizedPath<>(BasicPathTestCase.basicTripAsPath());

    // Verify all costs
    assertEquals(BasicPathTestCase.TOTAL_COST, path.generalizedCost());
    assertEquals(0, path.breakTieCost());
    assertEquals(BasicPathTestCase.TOTAL_COST, path.generalizedCostWaitTimeOptimized());
    assertEquals(66_00, path.transferPriorityCost());

    // And toString is the same (transfer priority cost added)
    assertEquals(
      BasicPathTestCase.BASIC_PATH_AS_STRING.replace("]", " $66pri]"),
      path.toString(this::stopIndexToName)
    );

    // Verify details
    assertEquals(
      BasicPathTestCase.BASIC_PATH_AS_DETAILED_STRING.replace("]", " $66pri]"),
      path.toStringDetailed(this::stopIndexToName)
    );

    // Make sure the toString does not throw an exception or return null
    assertNotNull(path.toString());
  }

  @Test
  void copyBasicPathWithCostsAndVerifyCosts() {
    var orgPath = BasicPathTestCase.basicTripAsPath();
    var accessLeg = orgPath.accessLeg();

    // Define some constants
    final int generalizedCost = 881100;
    final int transferPriorityCost = 120100;
    final int waitTimeOptimizedCost = 130200;
    final int generalizedCostWaitTimeOptimized = generalizedCost + waitTimeOptimizedCost;
    final int breakTieCost = 140300;

    var path = new OptimizedPath<>(
      accessLeg,
      orgPath.rangeRaptorIterationDepartureTime(),
      generalizedCost,
      transferPriorityCost,
      waitTimeOptimizedCost,
      breakTieCost
    );

    assertEquals(generalizedCost, path.generalizedCost());
    assertEquals(breakTieCost, path.breakTieCost());
    assertEquals(generalizedCostWaitTimeOptimized, path.generalizedCostWaitTimeOptimized());
    assertEquals(transferPriorityCost, path.transferPriorityCost());

    var exp = BasicPathTestCase.BASIC_PATH_AS_STRING.replace(
      "$8154]",
      "$8811 $1201pri " + "$" + (generalizedCostWaitTimeOptimized / 100) + "wtc]"
    );

    assertEquals(exp, path.toString(this::stopIndexToName));
  }
}
// 881 100 + 130 200 = 1 011 300
