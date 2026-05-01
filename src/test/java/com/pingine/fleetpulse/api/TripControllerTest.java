package com.pingine.fleetpulse.api;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pingine.fleetpulse.api.dto.TripResponse;
import com.pingine.fleetpulse.api.dto.VehicleResponse;
import com.pingine.fleetpulse.service.TripService;
import com.pingine.fleetpulse.service.VehicleNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

@WebMvcTest(TripController.class)
class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TripService tripService;

    @Test
    @DisplayName("Should return 200 and full JSON when trip exists")
    void shouldReturnTripWhenExists() throws Exception {
        String vehicleId = "v-123";

        TripResponse mockResponse = TripResponse.builder()
                .vehicle(VehicleResponse.builder()
                        .id(vehicleId)
                        .licensePlate("AA 1234 BB")
                        .model("Scania R450")
                        .vin("XPT123456789")
                        .driverName("Ivan Ivanov")
                        .build())
                .startedAt(Instant.parse("2026-05-01T10:00:00Z"))
                .endedAt(Instant.parse("2026-05-01T10:15:00Z"))
                .distanceKm(12.4)
                .avgSpeedKph(49.6)
                .pointCount(1)
                .points(List.of(
                        TripResponse.PointDto.builder()
                                .ts(Instant.parse("2026-05-01T10:05:00Z"))
                                .lat(55.75)
                                .lon(37.61)
                                .speedKph(50.0)
                                .build()
                ))
                .build();

        given(tripService.getLastTrip(vehicleId)).willReturn(mockResponse);

        mockMvc.perform(get("/api/v1/vehicles/{vehicleId}/last-trip", vehicleId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicle.id").value(vehicleId))
                .andExpect(jsonPath("$.vehicle.licensePlate").value("AA 1234 BB"))
                .andExpect(jsonPath("$.vehicle.driverName").value("Ivan Ivanov"))
                .andExpect(jsonPath("$.distanceKm").value(12.4))
                .andExpect(jsonPath("$.pointCount").value(1))
                .andExpect(jsonPath("$.points[0].speedKph").value(50.0));
    }

    @Test
    @DisplayName("Should return 404 when service throws VehicleNotFoundException")
    void shouldReturn404WhenNotFound() throws Exception {
        String vehicleId = "random";

        given(tripService.getLastTrip(vehicleId))
                .willThrow(new VehicleNotFoundException("Vehicle not found"));

        mockMvc.perform(get("/api/v1/vehicles/{vehicleId}/last-trip", vehicleId))
                .andExpect(status().isNotFound());
    }
}