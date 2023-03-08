package org.opentripplanner.ext.fares.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record FareLegRule(
  @Nullable String legGroupId,
  @Nullable String networkId,
  @Nullable String fromAreaId,
  @Nullable String toAreadId,
  @Nullable FareDistance fareDistance,
  @Nonnull FareProduct fareProduct,
  @Nullable Timeframe fromTimeFrame,
  @Nullable Timeframe toTimeframe
) {
  public FareLegRule(
    String legGroupId,
    String networkId,
    String fromAreaId,
    String toAreaId,
    FareProduct fareProduct
  ) {
    this(legGroupId, networkId, fromAreaId, toAreaId, null, fareProduct, null, null);
  }

  public FareLegRule(
    String legGroupId,
    String networkId,
    String fromAreaId,
    String toAreadId,
    FareDistance fareDistance,
    FareProduct fareProduct
  ) {
    this(legGroupId, networkId, fromAreaId, toAreadId, fareDistance, fareProduct, null, null);
  }
  public String feedId() {
    return fareProduct.id().getFeedId();
  }
}
