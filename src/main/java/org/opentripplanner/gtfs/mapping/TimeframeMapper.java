package org.opentripplanner.gtfs.mapping;

import java.time.LocalTime;
import java.util.Collection;
import org.opentripplanner.ext.fares.model.Timeframe;

public class TimeframeMapper {

  public Timeframe map(org.onebusaway.gtfs.model.Timeframe timeframe) {
    if (timeframe == null) return null;
    return new Timeframe(
      AgencyAndIdMapper.mapAgencyAndId(timeframe.getTimeFrameId()),
      LocalTime.parse(timeframe.getStartTime()),
      LocalTime.parse(timeframe.getEndTime())
    );
  }
}
