package org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper;

import static org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper.DigitransitVehicleRentalStationPropertyMapper.getFeedScopedIdAndNetwork;

import java.util.ArrayList;
import java.util.Collection;
import org.opentripplanner.api.mapping.PropertyMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalVehicle;

public class DigitransitRentalVehiclePropertyMapper extends PropertyMapper<VehicleRentalVehicle> {

  @Override
  protected Collection<KeyValue> map(VehicleRentalVehicle place) {
    var items = new ArrayList<KeyValue>();
    items.addAll(getFeedScopedIdAndNetwork(place));
    items.add(new KeyValue("formFactor", place.vehicleType.formFactor.toString()));
    return items;
  }
}
