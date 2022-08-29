package perftest;

import io.gatling.javaapi.core.Choice;
import io.gatling.javaapi.core.ScenarioBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.incrementUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.listFeeder;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.core.CoreDsl.tryMax;
import static io.gatling.javaapi.http.HttpDsl.http;
import static java.util.stream.Stream.concat;

public class ConcurrentSimulation extends BasicSimulation {

    private static final Logger log = LoggerFactory.getLogger(ConcurrentSimulation.class);
    public static final String RESERVE_ACTION = "reserve";
    public static final String CANCEL_RESERVATION_ACTION = "cancel";

    private int requestsPerSec = usersPerSec * maxSeats;
    private int howManyShows = 100000;


    private int howManyShows() {
        if (capacityLoadTesting.enabled) {
            int startingRate = capacityLoadTesting.from * maxSeats;
            int rateIncrement = capacityLoadTesting.step * maxSeats;
            return IntStream.range(0, capacityLoadTesting.times)
                    .map(iteration -> (startingRate + iteration * rateIncrement) * capacityLoadTesting.levelLastingSec)
                    .sum();
        } else {
            return requestsPerSec * duringSec;
        }
    }

    private List<String> showIds = IntStream.range(0, howManyShows)
            .mapToObj(__ -> UUID.randomUUID().toString()).toList();

    final AtomicInteger counter = new AtomicInteger();

    Iterator<Map<String, Object>> showIdsFeeder = showIds.stream().map(showId -> Collections.<String, Object>singletonMap("showId", showId)).iterator();

    List<Map<String, Object>> reserveOrCancelActions = showIds.stream()
            .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / requestsGroupingSize))
            .values().stream()
            .flatMap(showIds -> {
                log.debug("generating new batch of seats reservations for group size: " + showIds.size());
                List<Map<String, Object>> showReservations = prepareActions(showIds, RESERVE_ACTION);
                List<Map<String, Object>> showCancellations = prepareActions(showIds, CANCEL_RESERVATION_ACTION);
                return concat(showReservations.stream(), showCancellations.stream());
            })
            .toList();

    private List<Map<String, Object>> prepareActions(List<String> showIds, String action) {
        List<Map<String, Object>> showReservations = showIds.stream()
                .flatMap(showId ->
                        IntStream.range(0, maxSeats).boxed()
                                .map(seatNum -> Map.<String, Object>of("showId", showId, "seatNum", seatNum, "action", action)))
                .collect(Collectors.toList());
        Collections.shuffle(showReservations);
        return showReservations;
    }

    ScenarioBuilder createShows = scenario("Create show scenario")
            .feed(showIdsFeeder)
            .exec(http("create-show")
                    .post("/shows")
                    .body(createShowPayload)
            );

    ScenarioBuilder reserveSeatsOrCancelReservation = scenario("Reserve seats or cancel reservation")
            .feed(listFeeder(reserveOrCancelActions).circular())
            .doSwitch("#{action}").on(
                    Choice.withKey(RESERVE_ACTION, tryMax(5).on(exec(http("reserve-seat") //tryMax in case of concurrent reservation/cancellation
                            .patch("shows/#{showId}/seats/#{seatNum}")
                            .body(reserveSeatPayload)))),
                    Choice.withKey(CANCEL_RESERVATION_ACTION, tryMax(5).on(exec(http("cancel-reservation")
                            .patch("shows/#{showId}/seats/#{seatNum}")
                            .body(cancelReservationPayload))))
            );

    {
        log.info("Configuration: maxSeats={}, usersPerSec={}, duringSec={}, capacity={}, howManyShows={}", maxSeats, usersPerSec, duringSec, capacityLoadTesting, howManyShows);
        setUp(createShows.injectOpen(constantUsersPerSec(showCreationConcurrentUsers).during(howManyShows / showCreationConcurrentUsers)).andThen(
                reserveSeatsOrCancelReservation.injectOpen(constantUsersPerSec(requestsPerSec).during(duringSec))))
                .protocols(httpProtocol);
    }
}

