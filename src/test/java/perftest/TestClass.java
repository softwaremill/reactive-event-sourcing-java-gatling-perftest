package perftest;

import io.vavr.collection.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TestClass {
    private static final Logger log = LoggerFactory.getLogger(ConcurrentSimulation.class);
    private int showCreationConcurrentUsers = 500;
    private int usersPerSec = 50;
    private int maxSeats = 50;
    private int requestsPerSec = usersPerSec * maxSeats;
    private int duringSec = 300;
    private int howManyShows = (usersPerSec * duringSec);
    private List<String> showIds = List.range(0, howManyShows)
            .map(__ -> UUID.randomUUID().toString());


    Iterator<Map<String, Object>> showIdsFeeder = showIds.map(showId -> Collections.<String, Object>singletonMap("showId", showId)).iterator();
//    Iterator<Map<String, Object>> reservationsFeeder = showIds
//            .flatMap(showId -> {
//                        log.info("tworze");
//                        return List.range(0, maxSeats)
//                                .map(seatNum -> Map.<String, Object>of("showId", showId, "seatNum", seatNum))
//                                .shuffle();
//                    }
//            )
//            .iterator();

    Iterator<Map<String, Object>> reservationsFeeder2 = showIds.asJava().stream()
            .flatMap(showId -> {
                log.info("tworze");
                java.util.List<Map<String, Object>> showReservations = IntStream.range(0, maxSeats).boxed()
                        .map(seatNum -> Map.<String, Object>of("showId", showId, "seatNum", seatNum))
                        .collect(Collectors.toList());
                java.util.Collections.shuffle(showReservations);
                return showReservations.stream();
            })
            .iterator();
//            .flatMap(showId -> {
//                        log.info("tworze");
//                        return List.range(0, maxSeats)
//                                .map(seatNum -> Map.<String, Object>of("showId", showId, "seatNum", seatNum));
//                    }
//            )
//            .shuffle()
//            .iterator();

    public static void main(String[] args) {
        TestClass concurrentSimulation = new TestClass();
        while (concurrentSimulation.reservationsFeeder2.hasNext()){
            concurrentSimulation.reservationsFeeder2.next();
        }
    }
}
