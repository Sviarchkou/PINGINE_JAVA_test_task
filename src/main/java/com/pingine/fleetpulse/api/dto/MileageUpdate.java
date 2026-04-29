package com.pingine.fleetpulse.api.dto;

import lombok.Value;

@Value
public class MileageUpdate {
    String vehicleId;
    double deltaKm;
}
