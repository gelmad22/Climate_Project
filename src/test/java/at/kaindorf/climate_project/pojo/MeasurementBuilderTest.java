package at.kaindorf.climate_project.pojo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MeasurementBuilderTest {

    @Test
    void builderCreatesMeasurementWithAllValues() {
        Station station = Station.builder().id(7).build();
        LocalDateTime startTime = LocalDateTime.of(2026, 6, 15, 8, 0);
        LocalDateTime endTime = startTime.plusHours(1);
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 15, 8, 30);
        BigDecimal ozone = new BigDecimal("84.25");

        Measurement measurement = Measurement.builder()
                .id(12L)
                .station(station)
                .ozone(ozone)
                .startTime(startTime)
                .endTime(endTime)
                .createdAt(createdAt)
                .build();

        assertEquals(12L, measurement.getId());
        assertSame(station, measurement.getStation());
        assertEquals(ozone, measurement.getOzone());
        assertEquals(startTime, measurement.getStartTime());
        assertEquals(endTime, measurement.getEndTime());
        assertEquals(createdAt, measurement.getCreatedAt());
    }

    @Test
    void builderRejectsMissingRequiredFields() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> Measurement.builder().build()
        );

        assertEquals("Measurement station must not be null", exception.getMessage());
    }

    @Test
    void builderRejectsInvalidTimeRange() {
        Station station = Station.builder().id(7).build();
        LocalDateTime startTime = LocalDateTime.of(2026, 6, 15, 8, 0);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> Measurement.builder()
                        .station(station)
                        .ozone(new BigDecimal("84.25"))
                        .startTime(startTime)
                        .endTime(startTime)
                        .build()
        );

        assertEquals("Measurement start time must be before end time", exception.getMessage());
    }

    @Test
    void toDtoContainsOnlyAnnotatedFields() {
        LocalDateTime startTime = LocalDateTime.of(2026, 6, 15, 8, 0);
        Measurement measurement = Measurement.builder()
                .id(12L)
                .station(Station.builder().id(7).build())
                .ozone(new BigDecimal("84.25"))
                .startTime(startTime)
                .endTime(startTime.plusHours(1))
                .createdAt(startTime.plusMinutes(5))
                .build();

        Map<String, Object> dto = measurement.toDto();

        assertEquals(Set.of("ozone", "startTime", "endTime"), dto.keySet());
        assertEquals(new BigDecimal("84.25"), dto.get("ozone"));
        assertEquals(startTime, dto.get("startTime"));
        assertEquals(startTime.plusHours(1), dto.get("endTime"));
    }
}
