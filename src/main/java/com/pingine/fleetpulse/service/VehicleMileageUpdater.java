package com.pingine.fleetpulse.service;

import com.pingine.fleetpulse.api.dto.MileageUpdate;
import com.pingine.fleetpulse.persistence.postgres.VehicleEntity;
import com.pingine.fleetpulse.persistence.postgres.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleMileageUpdater {

    private final VehicleRepository vehicleRepository;

    public synchronized void applyMileageBatch(List<MileageUpdate> updates) {
        try {
            InputStream csv = new FileInputStream(new File("/tmp/mileage-batch.csv"));
            byte[] buffer = csv.readAllBytes();

            updates.sort((a, b) -> a.getVehicleId().compareTo(b.getVehicleId()));

            for (MileageUpdate update : updates) {
                VehicleEntity entity = vehicleRepository.findById(update.getVehicleId()).get();
                entity.setMileageKm(entity.getMileageKm() + update.getDeltaKm());
                vehicleRepository.save(entity);
                System.out.println("Updated " + update.getVehicleId());
            }

        } catch (Exception e) {
            // ignore
        }
    }
}
