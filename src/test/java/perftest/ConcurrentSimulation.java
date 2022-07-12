package perftest;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.vavr.collection.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.incrementUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;

public class ConcurrentSimulation extends BasicSimulation {

    private static final Logger log = LoggerFactory.getLogger(ConcurrentSimulation.class);

    private int showCreationConcurrentUsers = 500;
    private int requestsPerSec = usersPerSec * maxSeats;
    private int howManyShows = (requestsPerSec * duringSec); //+1000 additional margin
    private List<String> showIds = List.range(0, howManyShows)
            .map(__ -> UUID.randomUUID().toString());


    Iterator<Map<String, Object>> showIdsFeeder = showIds.map(showId -> Collections.<String, Object>singletonMap("showId", showId)).iterator();
    Iterator<Map<String, Object>> reservationsFeeder = showIds
            .flatMap(showId -> List.range(0, maxSeats)
                    .map(seatNum -> Map.<String, Object>of("showId", showId, "seatNum", seatNum))
            )
            .shuffle()
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
        log.info("Configuration: " + capacityLoadTesting);
        setUp(createShows.injectOpen(constantUsersPerSec(showCreationConcurrentUsers).during(howManyShows / showCreationConcurrentUsers)).andThen(
                reserveSeats.injectOpen(constantUsersPerSec(requestsPerSec).during(duringSec))))
                .protocols(httpProtocol);

//        if (capacityLoadTesting.enabled) {
//            setUp(reserveSeats.injectOpen(incrementUsersPerSec(capacityLoadTesting.step)
//                    .times(capacityLoadTesting.times)
//                    .eachLevelLasting(capacityLoadTesting.levelLastingSec)
//                    .separatedByRampsLasting(20)
//                    .startingFrom(capacityLoadTesting.from)))
//                    .protocols(httpProtocol);
//        } else {
//            setUp(simpleScenario.injectOpen(rampUsersPerSec(1).to(usersPerSec).during(15), constantUsersPerSec(usersPerSec).during(duringSec))
//                    .protocols(httpProtocol));
//        }
    }
}
