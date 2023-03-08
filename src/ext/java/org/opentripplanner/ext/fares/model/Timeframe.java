package org.opentripplanner.ext.fares.model;

import java.time.LocalTime;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public record Timeframe(FeedScopedId timeframeId, LocalTime startTime, LocalTime endTime) {}
