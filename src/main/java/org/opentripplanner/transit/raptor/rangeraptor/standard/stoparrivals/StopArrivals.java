package org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.TransitArrival;
import org.opentripplanner.transit.raptor.rangeraptor.internalapi.RoundProvider;
import org.opentripplanner.transit.raptor.rangeraptor.standard.BestNumberOfTransfers;
import org.opentripplanner.transit.raptor.rangeraptor.standard.DestinationArrivalListener;
import org.opentripplanner.transit.raptor.rangeraptor.transit.EgressPaths;

/**
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class StopArrivals<T extends RaptorTripSchedule> implements BestNumberOfTransfers {

  private final StopArrivalState<T>[][] arrivals;
  private final RoundProvider roundProvider;

  public StopArrivals(int nRounds, int nStops, RoundProvider roundProvider) {
    this.roundProvider = roundProvider;
    //noinspection unchecked
    this.arrivals = (StopArrivalState<T>[][]) new StopArrivalState[nRounds][nStops];
  }

  /**
   * Setup egress arrivals with a callback which is notified when a new transit egress arrival
   * happens.
   */
  public void setupEgressStopStates(
    EgressPaths egressPaths,
    DestinationArrivalListener destinationArrivalListener
  ) {
    for (int i = 1; i < arrivals.length; i++) {
      final int round = i;
      egressPaths
        .byStop()
        .forEachEntry((stop, list) -> {
          arrivals[round][stop] =
            new EgressStopArrivalState<>(stop, round, list, destinationArrivalListener);
          return true;
        });
    }
  }

  public StopArrivalState<T> get(int round, int stop) {
    return arrivals[round][stop];
  }

  @Override
  public int calculateMinNumberOfTransfers(int stop) {
    for (int i = 0; i < arrivals.length; i++) {
      if (arrivals[i][stop] != null) {
        return i - 1;
      }
    }
    return unreachedMinNumberOfTransfers();
  }

  void setAccessTime(int time, RaptorTransfer access, boolean bestTime) {
    final int stop = access.stop();
    var existingArrival = getOrCreateStopIndex(round(), stop);

    if (existingArrival instanceof AccessStopArrivalState) {
      ((AccessStopArrivalState<?>) existingArrival).setAccessTime(time, access, bestTime);
    } else {
      arrivals[round()][stop] =
        new AccessStopArrivalState<>(
          time,
          access,
          bestTime,
          (DefaultStopArrivalState<T>) existingArrival
        );
    }
  }

  /**
   * Set the time at a transit index iff it is optimal. This sets both the best time and the
   * transfer time
   */
  void transferToStop(int fromStop, RaptorTransfer transfer, int arrivalTime) {
    int stop = transfer.stop();
    var state = getOrCreateStopIndex(round(), stop);

    state.transferToStop(fromStop, arrivalTime, transfer);
  }

  void transitToStop(int stop, int time, int boardStop, int boardTime, T trip, boolean bestTime) {
    var state = getOrCreateStopIndex(round(), stop);

    state.arriveByTransit(time, boardStop, boardTime, trip);

    if (bestTime) {
      state.setBestTimeTransit(time);
    }
  }

  int bestTimePreviousRound(int stop) {
    return get(round() - 1, stop).time();
  }

  /* private methods */

  TransitArrival<T> previousTransit(int boardStopIndex) {
    final int prevRound = round() - 1;
    int stopIndex = boardStopIndex;
    StopArrivalState<T> state = get(prevRound, boardStopIndex);

    // We check for transfer before access, since a FLEX arrive on-board
    // can be followed by a transfer
    if (state.arrivedByTransfer()) {
      stopIndex = state.transferFromStop();
      state = arrivals[prevRound][stopIndex];
    }
    return state.arrivedByTransit()
      ? TransitArrival.create(state.trip(), stopIndex, state.onBoardArrivalTime())
      : null;
  }

  private int round() {
    return roundProvider.round();
  }

  private StopArrivalState<T> getOrCreateStopIndex(final int round, final int stop) {
    if (arrivals[round][stop] == null) {
      arrivals[round][stop] = StopArrivalState.create();
    }
    return get(round, stop);
  }
}
