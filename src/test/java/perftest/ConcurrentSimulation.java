package perftest;

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
import static io.gatling.javaapi.core.CoreDsl.incrementUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;

public class ConcurrentSimulation extends BasicSimulation {

    private static final Logger log = LoggerFactory.getLogger(ConcurrentSimulation.class);

    private int showCreationConcurrentUsers = 2000;
    private int requestsPerSec = usersPerSec * maxSeats;
    private int howManyShows = howManyShows();

    private int howManyShows() {
        if (capacityLoadTesting.enabled) {
            int startingRate = capacityLoadTesting.from * maxSeats;
            int rateIncrement = capacityLoadTesting.step * maxSeats;
            return IntStream.range(0, capacityLoadTesting.times)
                    .map(iteration -> (startingRate + iteration * rateIncrement) * capacityLoadTesting.levelLastingSec)
                    .sum();
        }else {
            return usersPerSec * duringSec;
        }
    }

    private List<String> showIds = IntStream.range(0, howManyShows)
            .mapToObj(__ -> UUID.randomUUID().toString()).toList();

    final int chunkSize = 5;
    final AtomicInteger counter = new AtomicInteger();


    Iterator<Map<String, Object>> showIdsFeeder = showIds.stream().map(showId -> Collections.<String, Object>singletonMap("showId", showId)).iterator();

    Iterator<Map<String, Object>> reservationsFeeder = showIds.stream()
            .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / chunkSize))
            .values().stream()
            .flatMap(showIds -> {
                log.debug("generating new batch of seats reservations for group size: " + showIds.size());
                List<Map<String, Object>> showReservations = showIds.stream().flatMap(showId -> {
                    return IntStream.range(0, maxSeats).boxed()
                            .map(seatNum -> Map.<String, Object>of("showId", showId, "seatNum", seatNum));
                }).collect(Collectors.toList());
                java.util.Collections.shuffle(showReservations);
                return showReservations.stream();
            })
            .iterator();

    ScenarioBuilder createShows = scenario("Create show scenario")
            .feed(showIdsFeeder)
            .exec(http("create-show")
                    .post("/shows")
                    .body(createShowPayload)
            );

    ScenarioBuilder reserveSeats = scenario("Reserve seats")
            .feed(reservationsFeeder)
            .exec(http("reserve-seat")
                    .patch("shows/#{showId}/seats/#{seatNum}")
                    .body(reserveSeatPayload));

    {
        log.info("Configuration: maxSeats={}, usersPerSec={}, duringSec={}, capacity={}, howManyShows={}", maxSeats, usersPerSec, duringSec, capacityLoadTesting, howManyShows);

        if (capacityLoadTesting.enabled) {
            int startingRate = capacityLoadTesting.from * maxSeats;
            int rateIncrement = capacityLoadTesting.step * maxSeats;

            setUp(createShows.injectOpen(constantUsersPerSec(showCreationConcurrentUsers).during(howManyShows / showCreationConcurrentUsers)).andThen(
                            reserveSeats.injectOpen(incrementUsersPerSec(rateIncrement)
                                    .times(capacityLoadTesting.times)
                                    .eachLevelLasting(capacityLoadTesting.levelLastingSec)
//                                    .separatedByRampsLasting(20)
                                    .startingFrom(startingRate)))
                    .protocols(httpProtocol));
        } else {
            setUp(createShows.injectOpen(constantUsersPerSec(showCreationConcurrentUsers).during(howManyShows / showCreationConcurrentUsers)).andThen(
                    reserveSeats.injectOpen(constantUsersPerSec(requestsPerSec).during(duringSec))))
                    .protocols(httpProtocol);
        }
    }
}
