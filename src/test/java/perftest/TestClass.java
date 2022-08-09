package perftest;

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
import java.util.stream.Stream;

public class TestClass {
    private static final Logger log = LoggerFactory.getLogger(ConcurrentSimulation.class);
    private int showCreationConcurrentUsers = 500;
    private int usersPerSec = 10;
    private int maxSeats = 5;
    private int requestsPerSec = usersPerSec * maxSeats;
    private int duringSec = 300;
    private int howManyShows = (usersPerSec * duringSec);
    private List<String> showIds = IntStream.range(0, howManyShows)
            .mapToObj(__ -> UUID.randomUUID().toString()).toList();

    final int chunkSize = 3;
    final AtomicInteger counter = new AtomicInteger();


    Iterator<Map<String, Object>> showIdsFeeder = showIds.stream().map(showId -> Collections.<String, Object>singletonMap("showId", showId)).iterator();
//    Iterator<Map<String, Object>> reservationsFeeder = showIds
//            .flatMap(showId -> {
//                        log.info("tworze");
//                        return List.range(0, maxSeats)
//                                .map(seatNum -> Map.<String, Object>of("showId", showId, "seatNum", seatNum))
//                                .shuffle();
//                    }
//            )
//            .iterator();

    Iterator<Map<String, Object>> reservationsFeeder2 = showIds.stream()
            .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / chunkSize))
            .values().stream()
            .flatMap(showIds -> {

                log.info("tworze dla grupy " + showIds.size() );
                List<Map<String, Object>> showReservations = showIds.stream().flatMap(showId -> {
                    return IntStream.range(0, maxSeats).boxed()
                            .map(seatNum -> Map.<String, Object>of("showId", showId, "seatNum", seatNum));
//                    return showReservations.stream();
                }).collect(Collectors.toList());


//                java.util.List<Map<String, Object>> showReservations = IntStream.range(0, maxSeats).boxed()
//                        .map(seatNum -> Map.<String, Object>of("showId", showId, "seatNum", seatNum))
//                        .collect(Collectors.toList());
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

        final int chunkSize = 3;
        final AtomicInteger counter = new AtomicInteger();

//        concurrentSimulation.showIds.stream()
////            .collect(Collectors.toList())
//                .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / chunkSize))
//                .values().stream().fl;

        while (concurrentSimulation.reservationsFeeder2.hasNext()){

            Map<String, Object> next = concurrentSimulation.reservationsFeeder2.next();
            System.out.println(next);
        }
    }
}
