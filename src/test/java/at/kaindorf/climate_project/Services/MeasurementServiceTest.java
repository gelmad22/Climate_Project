package at.kaindorf.climate_project.Services;

import at.kaindorf.climate_project.pojo.Measurement;
import at.kaindorf.climate_project.pojo.Station;
import at.kaindorf.climate_project.repositories.MeasurementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeasurementServiceTest {

    @Mock
    private MeasurementRepository measurementRepository;

    private MeasurementService measurementService;

    @BeforeEach
    void setUp() {
        measurementService = new MeasurementService(measurementRepository);
    }

    @Test
    void getMeasurementDtosBetweenAddsStationAndUnit() {
        LocalDateTime from = LocalDateTime.of(2026, 6, 15, 8, 0);
        LocalDateTime to = from.plusHours(2);
        Measurement measurement = measurement(7, "88.50", from);

        when(measurementRepository.findByStation_IdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
                7,
                from,
                to
        )).thenReturn(List.of(measurement));

        List<Map<String, Object>> result = measurementService.getMeasurementDtosBetween(from, to, 7);

        assertEquals(1, result.size());
        assertEquals(7, result.getFirst().get("stationId"));
        assertEquals(new BigDecimal("88.50"), result.getFirst().get("ozone"));
        assertEquals("\u00b5g/m\u00b3", result.getFirst().get("unit"));
        verify(measurementRepository).findByStation_IdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
                7,
                from,
                to
        );
        verifyNoMoreInteractions(measurementRepository);
    }

    @Test
    void getAverageBetweenRoundsAverageAndIncludesStationId() {
        LocalDateTime from = LocalDateTime.of(2026, 6, 15, 8, 0);
        LocalDateTime to = from.plusHours(2);

        when(measurementRepository.findByStation_IdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
                7,
                from,
                to
        )).thenReturn(List.of(measurement(7, "88.50", from)));
        when(measurementRepository.calculateAverageBetween(7, from, to)).thenReturn(88.555);

        Map<String, Object> result = measurementService.getAverageBetween(from, to, 7);

        assertEquals(from, result.get("from"));
        assertEquals(to, result.get("to"));
        assertEquals(7, result.get("stationId"));
        assertEquals(new BigDecimal("88.56"), result.get("average"));
        assertEquals("\u00b5g/m\u00b3", result.get("unit"));
        verify(measurementRepository).findByStation_IdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
                7,
                from,
                to
        );
        verify(measurementRepository).calculateAverageBetween(7, from, to);
        verifyNoMoreInteractions(measurementRepository);
    }

    @Test
    void getTopValuesBetweenRejectsNonPositiveLimit() {
        LocalDateTime from = LocalDateTime.of(2026, 6, 15, 8, 0);
        LocalDateTime to = from.plusHours(1);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> measurementService.getTopValuesBetween(from, to, 0, null)
        );

        assertEquals("Limit must be greater than 0", exception.getMessage());
        verifyNoInteractions(measurementRepository);
    }

    @Test
    void getMeasurementsBetweenRejectsInvalidRange() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 6, 15, 8, 0);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> measurementService.getMeasurementsBetween(timestamp, timestamp, null)
        );

        assertEquals("From must be before to", exception.getMessage());
        verifyNoInteractions(measurementRepository);
    }

    private Measurement measurement(int stationId, String ozone, LocalDateTime startTime) {
        Station station = Station.builder()
                .id(stationId)
                .build();

        return Measurement.builder()
                .station(station)
                .ozone(new BigDecimal(ozone))
                .startTime(startTime)
                .endTime(startTime.plusHours(1))
                .build();
    }
}
