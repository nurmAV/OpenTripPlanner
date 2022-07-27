package org.opentripplanner.routing.graphfinder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.routing.vertextype.VehicleRentalPlaceVertex;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitService;

class StreetGraphFinderTest extends GraphRoutingTest {

  private TransitStopVertex S1, S2, S3;
  private IntersectionVertex A, B, C, D;
  private VehicleRentalPlaceVertex BR1, BR2;
  private RoutingService routingService;

  private TransitService transitService;
  private StreetGraphFinder graphFinder;
  private Route R1, R2;
  private TripPattern TP1, TP2;
  private VehicleParking BP1, PR1, PR2;

  @BeforeEach
  protected void setUp() throws Exception {
    var otpModel = graphOf(
      new Builder() {
        @Override
        public void build() {
          var a = TransitModelForTest.agency("Agency");

          R1 = route("R1", TransitMode.BUS, a);
          R2 = route("R2", TransitMode.TRAM, a);

          S1 = stop("S1", 47.500, 19.001);
          S2 = stop("S2", 47.510, 19.001);
          S3 = stop("S3", 47.520, 19.001);

          BR1 = vehicleRentalStation("BR1", 47.500, 18.999);
          BR2 = vehicleRentalStation("BR2", 47.520, 18.999);

          A = intersection("A", 47.500, 19.00);
          B = intersection("B", 47.510, 19.00);
          C = intersection("C", 47.520, 19.00);
          D = intersection("D", 47.530, 19.00);

          BP1 =
            vehicleParking(
              "BP1",
              47.520,
              18.999,
              true,
              false,
              List.of(vehicleParkingEntrance(C, "BP1 Entrance", false, true))
            );

          PR1 =
            vehicleParking(
              "PR1",
              47.510,
              18.999,
              false,
              true,
              List.of(vehicleParkingEntrance(B, "PR1 Entrance", true, true))
            );

          PR2 =
            vehicleParking(
              "PR2",
              47.530,
              18.999,
              false,
              true,
              List.of(vehicleParkingEntrance(D, "PR2 Entrance", true, true))
            );

          biLink(A, S1);
          biLink(A, BR1);
          biLink(B, S2);
          biLink(C, S3);
          biLink(C, BR2);

          street(A, B, 100, StreetTraversalPermission.ALL);
          street(B, C, 100, StreetTraversalPermission.ALL);
          street(C, D, 100, StreetTraversalPermission.ALL);

          tripPattern(
            TP1 =
              new TripPattern(
                TransitModelForTest.id("TP1"),
                R1,
                new StopPattern(List.of(st(S1), st(S2)))
              )
          );
          tripPattern(
            TP2 =
              new TripPattern(
                TransitModelForTest.id("TP2"),
                R2,
                new StopPattern(List.of(st(S1), st(S3)))
              )
          );
        }
      }
    );

    var graph = otpModel.graph();
    var transitModel = otpModel.transitModel();

    routingService = new RoutingService(graph, transitModel);
    transitService = new DefaultTransitService(transitModel);
    graphFinder = new StreetGraphFinder(graph);
  }

  @Test
  void findClosestStops() {
    var ns1 = new NearbyStop(S1.getStop(), 0, null, null, null);
    var ns2 = new NearbyStop(S2.getStop(), 100, null, null, null);

    assertEquals(List.of(ns1), simplify(graphFinder.findClosestStops(47.500, 19.000, 10)));

    assertEquals(List.of(ns1, ns2), simplify(graphFinder.findClosestStops(47.500, 19.000, 100)));
  }

  @Test
  void findClosestPlacesLimiting() {
    var ns1 = new PlaceAtDistance(S1.getStop(), 0);
    var ns2 = new PlaceAtDistance(S2.getStop(), 100);
    var ns3 = new PlaceAtDistance(S3.getStop(), 200);
    var br1 = new PlaceAtDistance(BR1.getStation(), 0);
    var carParking = new PlaceAtDistance(PR1, 100);
    var bikeParking = new PlaceAtDistance(BP1, 200);
    var br2 = new PlaceAtDistance(BR2.getStation(), 200);
    var ps11 = new PlaceAtDistance(new PatternAtStop(S1.getStop(), TP1), 0);
    var ps21 = new PlaceAtDistance(new PatternAtStop(S1.getStop(), TP2), 0);

    assertEquals(
      List.of(ns1, ps21, ps11, br1),
      graphFinder.findClosestPlaces(
        47.500,
        19.000,
        10.0,
        10,
        null,
        null,
        null,
        null,
        null,
        routingService,
        transitService
      )
    );

    assertEquals(
      List.of(ns1, ps21, ps11, br1, carParking, ns2, bikeParking, ns3, br2),
      graphFinder.findClosestPlaces(
        47.500,
        19.000,
        200.0,
        100,
        null,
        null,
        null,
        null,
        null,
        routingService,
        transitService
      )
    );

    assertEquals(
      List.of(ns1, ps21, ps11),
      graphFinder.findClosestPlaces(
        47.500,
        19.000,
        200.0,
        3,
        null,
        null,
        null,
        null,
        null,
        routingService,
        transitService
      )
    );
  }

  @Test
  void findClosestPlacesWithAModeFilter() {
    var ns1 = new PlaceAtDistance(S1.getStop(), 0);
    var ns2 = new PlaceAtDistance(S2.getStop(), 100);
    var ns3 = new PlaceAtDistance(S3.getStop(), 200);

    assertEquals(
      List.of(ns1, ns2, ns3),
      graphFinder.findClosestPlaces(
        47.500,
        19.000,
        200.0,
        100,
        null,
        List.of(PlaceType.STOP),
        null,
        null,
        null,
        routingService,
        transitService
      )
    );

    assertEquals(
      List.of(ns1, ns2),
      graphFinder.findClosestPlaces(
        47.500,
        19.000,
        200.0,
        100,
        List.of(TransitMode.BUS),
        List.of(PlaceType.STOP),
        null,
        null,
        null,
        routingService,
        transitService
      )
    );
  }

  @Test
  void findClosestPlacesWithAStopFilter() {
    var ns1 = new PlaceAtDistance(S1.getStop(), 0);
    var ns2 = new PlaceAtDistance(S2.getStop(), 100);
    var ps11 = new PlaceAtDistance(new PatternAtStop(S1.getStop(), TP1), 0);
    var ps21 = new PlaceAtDistance(new PatternAtStop(S1.getStop(), TP2), 0);

    assertEquals(
      List.of(ns1, ps21, ps11, ns2),
      graphFinder.findClosestPlaces(
        47.500,
        19.000,
        100.0,
        100,
        null,
        List.of(PlaceType.STOP, PlaceType.PATTERN_AT_STOP),
        null,
        null,
        null,
        routingService,
        transitService
      )
    );

    assertEquals(
      List.of(ps21, ps11, ns2),
      graphFinder.findClosestPlaces(
        47.500,
        19.000,
        100.0,
        100,
        null,
        List.of(PlaceType.STOP, PlaceType.PATTERN_AT_STOP),
        List.of(S2.getStop().getId()),
        null,
        null,
        routingService,
        transitService
      )
    );
  }

  @Test
  void findClosestPlacesWithAStopAndRouteFilter() {
    var ns1 = new PlaceAtDistance(S1.getStop(), 0);
    var ns2 = new PlaceAtDistance(S2.getStop(), 100);
    var ps11 = new PlaceAtDistance(new PatternAtStop(S1.getStop(), TP1), 0);
    var ps21 = new PlaceAtDistance(new PatternAtStop(S1.getStop(), TP2), 0);

    assertEquals(
      List.of(ns1, ps21, ps11, ns2),
      graphFinder.findClosestPlaces(
        47.500,
        19.000,
        100.0,
        100,
        null,
        List.of(PlaceType.STOP, PlaceType.PATTERN_AT_STOP),
        null,
        null,
        null,
        routingService,
        transitService
      )
    );

    assertEquals(
      List.of(ps11, ns2),
      graphFinder.findClosestPlaces(
        47.500,
        19.000,
        100.0,
        100,
        null,
        List.of(PlaceType.STOP, PlaceType.PATTERN_AT_STOP),
        List.of(S2.getStop().getId()),
        List.of(R1.getId()),
        null,
        routingService,
        transitService
      )
    );
  }

  @Test
  void findClosestPlacesWithARouteFilter() {
    var ns1 = new PlaceAtDistance(S1.getStop(), 0);
    var ns2 = new PlaceAtDistance(S2.getStop(), 100);
    var ns3 = new PlaceAtDistance(S3.getStop(), 200);
    var ps11 = new PlaceAtDistance(new PatternAtStop(S1.getStop(), TP1), 0);
    var ps21 = new PlaceAtDistance(new PatternAtStop(S1.getStop(), TP2), 0);

    assertEquals(
      List.of(ns1, ps21, ps11, ns2, ns3),
      graphFinder.findClosestPlaces(
        47.500,
        19.000,
        200.0,
        100,
        null,
        List.of(PlaceType.STOP, PlaceType.PATTERN_AT_STOP),
        null,
        null,
        null,
        routingService,
        transitService
      )
    );

    assertEquals(
      List.of(ns1, ps21, ns2, ns3),
      graphFinder.findClosestPlaces(
        47.500,
        19.000,
        200.0,
        100,
        null,
        List.of(PlaceType.STOP, PlaceType.PATTERN_AT_STOP),
        null,
        List.of(R2.getId()),
        null,
        routingService,
        transitService
      )
    );
  }

  @Test
  void findClosestPlacesWithAVehicleRentalFilter() {
    var br1 = new PlaceAtDistance(BR1.getStation(), 0);
    var br2 = new PlaceAtDistance(BR2.getStation(), 200);

    assertEquals(
      List.of(br1, br2),
      graphFinder.findClosestPlaces(
        47.500,
        19.000,
        300.0,
        100,
        null,
        List.of(PlaceType.VEHICLE_RENT),
        null,
        null,
        null,
        routingService,
        transitService
      )
    );

    assertEquals(
      List.of(br2),
      graphFinder.findClosestPlaces(
        47.500,
        19.000,
        300.0,
        100,
        null,
        List.of(PlaceType.VEHICLE_RENT),
        null,
        null,
        List.of("BR2"),
        routingService,
        transitService
      )
    );
  }

  @Test
  void findClosestPlacesWithABikeParkFilter() {
    var bikeParking = new PlaceAtDistance(BP1, 200);

    assertEquals(
      List.of(bikeParking),
      graphFinder.findClosestPlaces(
        47.500,
        19.000,
        300.0,
        100,
        null,
        List.of(PlaceType.BIKE_PARK),
        null,
        null,
        null,
        routingService,
        transitService
      )
    );
  }

  @Test
  void findClosestPlacesWithACarParkFilter() {
    var parkAndRides = List.of(new PlaceAtDistance(PR1, 100), new PlaceAtDistance(PR2, 300));

    assertEquals(
      parkAndRides,
      graphFinder.findClosestPlaces(
        47.500,
        19.000,
        300.0,
        100,
        null,
        List.of(PlaceType.CAR_PARK),
        null,
        null,
        null,
        routingService,
        transitService
      )
    );
  }

  private List<NearbyStop> simplify(List<NearbyStop> closestStops) {
    return closestStops
      .stream()
      .map(ns -> new NearbyStop(ns.stop, ns.distance, null, null, null))
      .collect(Collectors.toList());
  }
}
