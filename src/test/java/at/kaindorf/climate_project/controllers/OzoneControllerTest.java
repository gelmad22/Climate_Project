package at.kaindorf.climate_project.controllers;

import at.kaindorf.climate_project.Services.MeasurementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OzoneControllerTest {

    @Mock
    private MeasurementService measurementService;

    private OzoneController ozoneController;

    @BeforeEach
    void setUp() {
        ozoneController = new OzoneController(measurementService);
    }

    @Test
    void getAverageParsesSupportedDateFormats() {
        LocalDateTime from = LocalDateTime.of(2026, 6, 15, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 16, 8, 30);
        Map<String, Object> expected = Map.of("average", new BigDecimal("88.50"));

        when(measurementService.getAverageBetween(from, to, 7)).thenReturn(expected);

        Map<String, Object> result = ozoneController.getAverage("2026-06-15", "2026-06-16 08:30:00", 7);

        assertSame(expected, result);
        verify(measurementService).getAverageBetween(from, to, 7);
    }

    @Test
    void getMeasurementParsesIsoDateTime() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 6, 15, 8, 30);
        Map<String, Object> expected = Map.of("ozone", new BigDecimal("88.50"));

        when(measurementService.getDtoByStartTime(timestamp, null)).thenReturn(expected);

        Map<String, Object> result = ozoneController.getMeasurement("2026-06-15T08:30:00", null);

        assertSame(expected, result);
        verify(measurementService).getDtoByStartTime(timestamp, null);
    }

    @Test
    void handleIllegalArgumentExceptionCreatesBadRequestBody() {
        Map<String, Object> response = ozoneController.handleIllegalArgumentException(
                new IllegalArgumentException("From must be before to")
        );

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.get("status"));
        assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), response.get("error"));
        assertEquals("From must be before to", response.get("message"));
    }
}
