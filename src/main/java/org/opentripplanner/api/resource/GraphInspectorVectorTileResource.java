package org.opentripplanner.api.resource;

import static org.opentripplanner.framework.io.HttpUtils.APPLICATION_X_PROTOBUF;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import org.glassfish.grizzly.http.server.Request;
import org.opentripplanner.api.model.TileJson;
import org.opentripplanner.inspector.vector.AreaStopsLayerBuilder;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.inspector.vector.VectorTileResponseFactory;
import org.opentripplanner.inspector.vector.geofencing.GeofencingZonesLayerBuilder;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.service.TransitService;

/**
 * Slippy map vector tile API for rendering various graph information for inspection/debugging
 * purposes.
 */
@Path("/routers/{ignoreRouterId}/inspector/vectortile")
public class GraphInspectorVectorTileResource {

  private static final List<LayerParameters<LayerType>> DEBUG_LAYERS = List.of(
    new LayerParams("areaStops", LayerType.AreaStop),
    new LayerParams("geofencingZones", LayerType.GeofencingZones)
  );

  private final OtpServerRequestContext serverContext;
  private final String ignoreRouterId;

  public GraphInspectorVectorTileResource(
    @Context OtpServerRequestContext serverContext,
    /**
     * @deprecated The support for multiple routers are removed from OTP2.
     * See https://github.com/opentripplanner/OpenTripPlanner/issues/2760
     */
    @Deprecated @PathParam("ignoreRouterId") String ignoreRouterId
  ) {
    this.serverContext = serverContext;
    this.ignoreRouterId = ignoreRouterId;
  }

  @GET
  @Path("/{layers}/{z}/{x}/{y}.pbf")
  @Produces(APPLICATION_X_PROTOBUF)
  public Response tileGet(
    @Context Request grizzlyRequest,
    @PathParam("x") int x,
    @PathParam("y") int y,
    @PathParam("z") int z,
    @PathParam("layers") String requestedLayers
  ) {
    return VectorTileResponseFactory.create(
      x,
      y,
      z,
      grizzlyRequest.getLocale(),
      Arrays.asList(requestedLayers.split(",")),
      DEBUG_LAYERS,
      GraphInspectorVectorTileResource::createLayerBuilder,
      serverContext.graph(),
      serverContext.transitService()
    );
  }

  @GET
  @Path("/{layers}/tilejson.json")
  @Produces(MediaType.APPLICATION_JSON)
  public TileJson getTileJson(
    @Context UriInfo uri,
    @Context HttpHeaders headers,
    @PathParam("layers") String requestedLayers
  ) {
    var envelope = serverContext.worldEnvelopeService().envelope().orElseThrow();
    List<FeedInfo> feedInfos = serverContext
      .transitService()
      .getFeedIds()
      .stream()
      .map(serverContext.transitService()::getFeedInfo)
      .filter(Predicate.not(Objects::isNull))
      .toList();

    return new TileJson(
      uri,
      headers,
      requestedLayers,
      ignoreRouterId,
      "inspector/vectortile",
      envelope,
      feedInfos
    );
  }

  private static LayerBuilder<?> createLayerBuilder(
    LayerParameters<LayerType> layerParameters,
    Locale locale,
    Graph graph,
    TransitService transitService
  ) {
    return switch (layerParameters.type()) {
      case AreaStop -> new AreaStopsLayerBuilder(transitService, layerParameters, locale);
      case GeofencingZones -> new GeofencingZonesLayerBuilder(graph, layerParameters);
    };
  }

  private enum LayerType {
    AreaStop,
    GeofencingZones,
  }

  private record LayerParams(String name, LayerType type) implements LayerParameters<LayerType> {
    @Override
    public String mapper() {
      return "DebugClient";
    }
  }
}
