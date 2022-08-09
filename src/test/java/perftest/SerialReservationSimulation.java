package perftest;

import io.gatling.javaapi.core.ScenarioBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.incrementUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.rampUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;

public class SerialReservationSimulation extends BasicSimulation {

    private static final Logger log = LoggerFactory.getLogger(SerialReservationSimulation.class);

    ScenarioBuilder simpleScenario = scenario("Create show and reserve seats")
            .feed(showIdsFeeder)
            .exec(http("create-show")
                    .post("/shows")
                    .body(createShowPayload)
            )
            .foreach(randomSeatNums.asJava(), "seatNum").on(
                    exec(http("reserve-seat")
                            .patch("shows/#{showId}/seats/#{seatNum}")
                            .body(reserveSeatPayload))
            );

    //30 * 50 = 1500 req/s
//1 res 1 mi
    // calos 50 ms
    {
        log.info("Configuration: " + capacityLoadTesting);
        if (capacityLoadTesting.enabled) {
            setUp(simpleScenario.injectOpen(incrementUsersPerSec(capacityLoadTesting.step)
                    .times(capacityLoadTesting.times)
                    .eachLevelLasting(capacityLoadTesting.levelLastingSec)
                    .separatedByRampsLasting(20)
                    .startingFrom(capacityLoadTesting.from)))
                    .protocols(httpProtocol);
        } else {
            setUp(simpleScenario.injectOpen(rampUsersPerSec(1).to(usersPerSec).during(15), constantUsersPerSec(usersPerSec).during(duringSec))
                    .protocols(httpProtocol));
        }
    }
}
