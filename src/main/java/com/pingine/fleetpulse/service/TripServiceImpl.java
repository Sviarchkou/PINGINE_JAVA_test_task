package com.pingine.fleetpulse.service;

import com.pingine.fleetpulse.api.dto.TripResponse;
import com.pingine.fleetpulse.api.dto.VehicleResponse;
import com.pingine.fleetpulse.domain.Trip;
import com.pingine.fleetpulse.persistence.mongo.TelemetryPoint;
import com.pingine.fleetpulse.persistence.mongo.TelemetryRepository;
import com.pingine.fleetpulse.service.trip.TripDetector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TelemetryRepository telemetryRepository;
    private final TripDetector tripDetector;
    private final VehicleService vehicleService;

    @Override
    public TripResponse getLastTrip(String vehicleId) {
        List<TelemetryPoint> telemetryPoints = telemetryRepository.findRecentPoints(vehicleId, 100);
        if (telemetryPoints.isEmpty())
            throw new VehicleNotFoundException("Vehicle not found");
        List<Trip> trips = tripDetector.detect(telemetryPoints);
        Trip lastTrip = trips.get(trips.size() - 1);
        VehicleResponse vehicleResponse = vehicleService.getById(lastTrip.getVehicleId());

        List<TripResponse.PointDto> pointDtos = new ArrayList<>();
        lastTrip.getPoints().forEach(p ->
                pointDtos.add(TripResponse.PointDto.builder()
                        .ts(p.getTs())
                        .lat(p.getLat())
                        .lon(p.getLon())
                        .speedKph(p.getSpeedKph())
                        .build())
        );

        return TripResponse.builder()
                .vehicle(vehicleResponse)
                .startedAt(lastTrip.getStartedAt())
                .endedAt(lastTrip.getEndedAt())
                .distanceKm(lastTrip.getDistanceKm())
                .avgSpeedKph(lastTrip.getAvgSpeedKph())
                .pointCount(lastTrip.getPoints().size())
                .points(pointDtos)
                .build();
    }
}
