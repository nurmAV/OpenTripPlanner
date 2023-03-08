package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.Objects;
import org.opentripplanner.ext.fares.model.Distance;
import org.opentripplanner.ext.fares.model.FareDistance;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareProduct;
import org.opentripplanner.ext.fares.model.Timeframe;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;

public final class FareLegRuleMapper {

  private final FareProductMapper fareProductMapper;
  private final TimeframeMapper timeframeMapper;
  private final DataImportIssueStore issueStore;

  public FareLegRuleMapper(
    FareProductMapper fareProductMapper,
    TimeframeMapper timeframeMapper,
    DataImportIssueStore issueStore
  ) {
    this.fareProductMapper = fareProductMapper;
    this.timeframeMapper = timeframeMapper;
    this.issueStore = issueStore;
  }

  public Collection<FareLegRule> map(
    Collection<org.onebusaway.gtfs.model.FareLegRule> allFareLegRules
  ) {
    return allFareLegRules
      .stream()
      .map(r -> {
        FareProduct productForRule = fareProductMapper.map(r.getFareProduct());
        FareDistance fareDistance = createFareDistance(r);
        Timeframe toTimeFrame = timeframeMapper.map(r.getToTimeframeId());
        Timeframe fromTimeframe = timeframeMapper.map(r.getFromTimeframeId());

        if (productForRule != null) {
          return new FareLegRule(
            r.getLegGroupId(),
            r.getNetworkId(),
            r.getFromAreaId(),
            r.getToAreaId(),
            fareDistance,
            productForRule,
            toTimeFrame,
            fromTimeframe
          );
        } else {
          issueStore.add(
            "UnknownFareProductId",
            "Fare leg rule %s refers to unknown fare product %s",
            r.getId(),
            r.getFareProduct().getId()
          );
          return null;
        }
      })
      .filter(Objects::nonNull)
      .toList();
  }

  private FareDistance createFareDistance(org.onebusaway.gtfs.model.FareLegRule fareLegRule) {
    Double min = Objects.requireNonNullElse(fareLegRule.getMinDistance(), 0d);
    Double max = Objects.requireNonNullElse(fareLegRule.getMaxDistance(), Double.POSITIVE_INFINITY);

    return switch (fareLegRule.getDistanceType()) {
      case 0 -> new FareDistance.Stops(min.intValue(), max.intValue());
      case 1 -> new FareDistance.LinearDistance(Distance.ofMeters(min), Distance.ofMeters(max));
      default -> null;
    };
  }
}
