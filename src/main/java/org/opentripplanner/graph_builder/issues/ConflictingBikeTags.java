package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public record ConflictingBikeTags(long wayId) implements DataImportIssue {
  private static String FMT =
    "Conflicting tags bicycle:[yes|designated] and cycleway: " +
    "dismount on way %s, assuming dismount";
  private static String HTMLFMT =
    "Conflicting tags bicycle:[yes|designated] and cycleway: " +
    "dismount on way <a href=\"http://www.openstreetmap.org/way/%d\">\"%d\"</a>, assuming dismount";

  @Override
  public String getMessage() {
    return String.format(FMT, wayId);
  }

  @Override
  public String getHTMLMessage() {
    if (wayId > 0) {
      return String.format(HTMLFMT, wayId, wayId);
      // If way is lower then 0 it means it is temporary ID and so useless to link to OSM
    } else {
      return getMessage();
    }
  }
}
