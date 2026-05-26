package at.kaindorf.climate_project.controllers;

import at.kaindorf.climate_project.Services.MeasurementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/ozone")
@RequiredArgsConstructor
public class OzoneController {

    private final MeasurementService measurementService;

    @GetMapping("/measurements")
    public List<Map<String, Object>> getMeasurements(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getMeasurementDtosBetween(parseDateTime(from), parseDateTime(to), stationId);
    }

    @GetMapping("/measurement/{timestamp}")
    public Map<String, Object> getMeasurement(
            @PathVariable String timestamp,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getDtoByStartTime(parseDateTime(timestamp), stationId);
    }

    @GetMapping("/average")
    public Map<String, Object> getAverage(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getAverageBetween(parseDateTime(from), parseDateTime(to), stationId);
    }

    @GetMapping("/average/day")
    public List<Map<String, Object>> getAverageByDay(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getAverageByDay(parseDateTime(from), parseDateTime(to), stationId);
    }

    @GetMapping("/average/week")
    public List<Map<String, Object>> getAverageByWeek(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getAverageByWeek(parseDateTime(from), parseDateTime(to), stationId);
    }

    @GetMapping("/average/month")
    public List<Map<String, Object>> getAverageByMonth(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getAverageByMonth(parseDateTime(from), parseDateTime(to), stationId);
    }

    @GetMapping("/average/year")
    public List<Map<String, Object>> getAverageByYear(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getAverageByYear(parseDateTime(from), parseDateTime(to), stationId);
    }

    @GetMapping("/min")
    public Map<String, Object> getMin(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getMinBetween(parseDateTime(from), parseDateTime(to), stationId);
    }

    @GetMapping("/min/day")
    public List<Map<String, Object>> getMinByDay(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getMinByDay(parseDateTime(from), parseDateTime(to), stationId);
    }

    @GetMapping("/min/week")
    public List<Map<String, Object>> getMinByWeek(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getMinByWeek(parseDateTime(from), parseDateTime(to), stationId);
    }

    @GetMapping("/min/month")
    public List<Map<String, Object>> getMinByMonth(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getMinByMonth(parseDateTime(from), parseDateTime(to), stationId);
    }

    @GetMapping("/min/year")
    public List<Map<String, Object>> getMinByYear(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getMinByYear(parseDateTime(from), parseDateTime(to), stationId);
    }

    @GetMapping("/max")
    public Map<String, Object> getMax(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getMaxBetween(parseDateTime(from), parseDateTime(to), stationId);
    }

    @GetMapping("/max/day")
    public List<Map<String, Object>> getMaxByDay(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getMaxByDay(parseDateTime(from), parseDateTime(to), stationId);
    }

    @GetMapping("/max/week")
    public List<Map<String, Object>> getMaxByWeek(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getMaxByWeek(parseDateTime(from), parseDateTime(to), stationId);
    }

    @GetMapping("/max/month")
    public List<Map<String, Object>> getMaxByMonth(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getMaxByMonth(parseDateTime(from), parseDateTime(to), stationId);
    }

    @GetMapping("/max/year")
    public List<Map<String, Object>> getMaxByYear(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getMaxByYear(parseDateTime(from), parseDateTime(to), stationId);
    }

    @GetMapping("/range")
    public Map<String, Object> getRange(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getRangeBetween(parseDateTime(from), parseDateTime(to), stationId);
    }

    @GetMapping("/top")
    public List<Map<String, Object>> getTop(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getTopValuesBetween(parseDateTime(from), parseDateTime(to), limit, stationId);
    }

    @GetMapping("/critical")
    public List<Map<String, Object>> getCritical(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "120") int threshold,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getCriticalMeasurementsBetween(
                parseDateTime(from),
                parseDateTime(to),
                threshold,
                stationId
        );
    }

    @GetMapping("/month-ranking")
    public List<Map<String, Object>> getMonthRanking(
            @RequestParam int year,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getMonthRanking(year, stationId);
    }

    @GetMapping("/year-comparison")
    public List<Map<String, Object>> getYearComparison(
            @RequestParam int fromYear,
            @RequestParam int toYear,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getYearComparison(fromYear, toYear, stationId);
    }

    @GetMapping("/critical-days")
    public Map<String, Object> getCriticalDays(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getCriticalDays(parseDateTime(from), parseDateTime(to), stationId);
    }

    @GetMapping("/longest-critical-period")
    public Map<String, Object> getLongestCriticalPeriod(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getLongestCriticalPeriod(parseDateTime(from), parseDateTime(to), stationId);
    }

    @GetMapping("/trend")
    public Map<String, Object> getTrend(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getTrend(parseDateTime(from), parseDateTime(to), stationId);
    }

    @GetMapping("/report")
    public Map<String, Object> getReport(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getStatisticsReport(parseDateTime(from), parseDateTime(to), stationId);
    }

    private LocalDateTime parseDateTime(String value) {
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIllegalArgumentException(IllegalArgumentException e) {
        return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(DateTimeParseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleDateTimeParseException(DateTimeParseException e) {
        return errorResponse(HttpStatus.BAD_REQUEST, "Invalid date/time: " + e.getParsedString());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNoSuchElementException(NoSuchElementException e) {
        return errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    private Map<String, Object> errorResponse(HttpStatus status, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("message", message);
        return response;
    }
}
