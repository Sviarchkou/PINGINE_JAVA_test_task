package com.pingine.fleetpulse.service.trip;

import com.pingine.fleetpulse.domain.Trip;
import com.pingine.fleetpulse.persistence.mongo.TelemetryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Splits a stream of telemetry points into completed trips.
 * A trip starts on ignition=true and ends on the next ignition=false.
 */
@Component
public class TripDetector {

    public List<Trip> detect(List<TelemetryPoint> points) {
        Map<String, Set<TelemetryPoint>> map = new HashMap<>();

        points.forEach(point -> {
            if (map.containsKey(point.getVehicleId())) {
                Set<TelemetryPoint> t = map.get(point.getVehicleId());
                t.add(point);
            } else {
                Set<TelemetryPoint> t = new LinkedHashSet<>();
                t.add(point);
                map.put(point.getVehicleId(), t);
            }
        });

        List<Trip> trips = new ArrayList<>();

        map.forEach((s, telemetryPointsSet) -> {

            List<TelemetryPoint> telemetryPoints = new ArrayList<>(telemetryPointsSet);
            telemetryPoints.sort(Comparator.comparing(TelemetryPoint::getTs)); // optional

            List<TelemetryPoint> currentTripPoints = new ArrayList<>();

            boolean inTrip = false;

            for (TelemetryPoint point : telemetryPoints) {
                if (!currentTripPoints.isEmpty()) {
                    TelemetryPoint lastAdded = currentTripPoints.get(currentTripPoints.size() - 1);
                    if (point.getTs().equals(lastAdded.getTs())) {
                        continue;
                    }
                }
                if (point.isIgnition()) {
                    if (!inTrip) {
                        inTrip = true;
                    }
                    currentTripPoints.add(point);
                } else if (inTrip) {
                    currentTripPoints.add(point);
                    trips.add(buildTrip(currentTripPoints));

                    currentTripPoints = new ArrayList<>();
                    inTrip = false;
                }
            }
        });

        return trips;
    }

    private Trip buildTrip(List<TelemetryPoint> points) {
        if (points.isEmpty()) return null;

        TelemetryPoint first = points.get(0);
        TelemetryPoint last = points.get(points.size() - 1);

        double totalDistance = 0;
        Instant startTime = first.getTs().toInstant(ZoneOffset.UTC);
        Instant endTime = last.getTs().toInstant(ZoneOffset.UTC);
        Duration duration = Duration.between(startTime, endTime);

        List<Trip.TripPoint> tripPoints = new ArrayList<>();

        for (int i = 0; i < points.size(); i++) {
            TelemetryPoint p = points.get(i);

            if (i > 0) {
                TelemetryPoint prev = points.get(i - 1);
                totalDistance += GeoDistance.haversineKm(
                        prev.getLat(), prev.getLon(),
                        p.getLat(), p.getLon()
                );
            }

            tripPoints.add(Trip.TripPoint.builder()
                    .ts(p.getTs().toInstant(ZoneOffset.UTC))
                    .lat(p.getLat())
                    .lon(p.getLon())
                    .speedKph(p.getSpeed())
                    .build());
        }

        double avgSpeed = totalDistance / duration.toMillis() * 3600000;

        return Trip.builder()
                .vehicleId(first.getVehicleId())
                .startedAt(first.getTs().toInstant(ZoneOffset.UTC))
                .endedAt(last.getTs().toInstant(ZoneOffset.UTC))
                .distanceKm(totalDistance)
                .avgSpeedKph(avgSpeed)
                .points(tripPoints)
                .build();
    }

}
