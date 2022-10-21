package org.opentripplanner.routing.algorithm.via;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.RouteViaRequest;
import org.opentripplanner.routing.api.request.request.ViaLocation;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.ViaRoutingResponse;

public class ViaRoutingWorker {

  private final RouteViaRequest viaRequest;
  private final RouteRequest request;
  private final Function<RouteRequest, RoutingResponse> routingWorker;

  public ViaRoutingWorker(
    RouteViaRequest request,
    Function<RouteRequest, RoutingResponse> routingWorker
  ) {
    this.request = request.routeRequest().clone();
    this.viaRequest = request;
    this.routingWorker = routingWorker;
  }

  public ViaRoutingResponse route() {
    /**
     * Loop over Via, for each cycle change from/to and JourneyRequest.
     */
    var result = viaRequest.viaLegs().stream().map(v -> getRoutingResponse(viaRequest, v)).toList();

    return combineRoutingResponse(result);
  }

  /**
   * Set to point and search trips. Return result with itineraries and prepare request for next
   * search.
   */
  private RoutingResponse getRoutingResponse(RouteViaRequest request, RouteViaRequest.ViaLeg v) {
    // If viaLocation is null then we are at last search
    if (v.viaLocation() != null) {
      this.request.setTo(v.viaLocation().point());
    } else {
      this.request.setTo(request.routeRequest().to());
    }

    var response = this.routingWorker.apply(this.request); //new RoutingWorker(serverContext, this.request, serverContext.transitService().getTimeZone()).route();
    var firstArrival = firstArrival(response).orElseThrow();
    var lastArrival = lastArrival(response).orElseThrow();
    var maxSlack = Optional
      .ofNullable(v.viaLocation())
      .map(ViaLocation::maxSlack)
      .orElse(Duration.ZERO);

    if (v.viaLocation() == null) {
      return response;
    }

    // Prepare next search
    var searchWindow = Duration.between(firstArrival, lastArrival).plus(maxSlack);

    this.request.setSearchWindow(searchWindow);
    this.request.setDateTime(firstArrival.plus(v.viaLocation().minSlack()).toInstant());
    this.request.setFrom(v.viaLocation().point());
    this.request.setJourney(v.journeyRequest());

    return response;
  }

  /**
   * For each itinerary from list element find itinerraries that can be combined to next list
   * element.
   */
  private ViaRoutingResponse combineRoutingResponse(List<RoutingResponse> routingResponses) {
    var res = new HashMap<Itinerary, List<Itinerary>>();
    var routingErrors = new ArrayList<RoutingError>();

    for (int i = 0; i < routingResponses.size() - 1; i++) {
      var errors = routingResponses.get(i).getRoutingErrors();

      if (errors != null) {
        routingErrors.addAll(errors);
      }

      for (Itinerary itinerary : routingResponses.get(i).getTripPlan().itineraries) {
        var filteredTransits = filterTransits(
          itinerary,
          routingResponses.get(i + 1).getTripPlan().itineraries,
          this.viaRequest.viaLegs().get(i).viaLocation()
        );

        if (!filteredTransits.isEmpty()) {
          res.put(itinerary, filteredTransits);
        }
      }
    }

    return new ViaRoutingResponse(res, routingResponses, routingErrors);
  }

  private List<Itinerary> filterTransits(
    Itinerary i,
    List<Itinerary> itineraries,
    ViaLocation viaLocation
  ) {
    return itineraries.stream().filter(predicate(i, viaLocation)).toList();
  }

  /**
   * Only allow departures within min/max slack time.
   */
  private Predicate<Itinerary> predicate(Itinerary i, ViaLocation v) {
    var earliestDeparturetime = i.endTime().plus(v.minSlack());
    var latestDeparturetime = i.endTime().plus(v.maxSlack());

    // Not before earlist and not after latest to include equal time
    return j ->
      !j.startTime().isBefore(earliestDeparturetime) && !j.startTime().isAfter(latestDeparturetime);
  }

  private Optional<ZonedDateTime> firstArrival(RoutingResponse response) {
    return Optional
      .ofNullable(response.getTripPlan())
      .map(t -> t.itineraries)
      .flatMap(i -> i.stream().min(Comparator.comparing(Itinerary::endTime)))
      .map(Itinerary::endTime);
  }

  private Optional<ZonedDateTime> lastArrival(RoutingResponse response) {
    return Optional
      .ofNullable(response.getTripPlan())
      .map(t -> t.itineraries)
      .flatMap(i -> i.stream().max(Comparator.comparing(Itinerary::endTime)))
      .map(Itinerary::endTime);
  }
}
