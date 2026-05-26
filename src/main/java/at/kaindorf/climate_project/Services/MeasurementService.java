package at.kaindorf.climate_project.services;

import at.kaindorf.climate_project.repositories.MeasurementRepository;
import at.kaindorf.climate_project.pojo.Measurement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class MeasurementService {

    private final MeasurementRepository measurementRepository;

    private static final String UNIT = "µg/m³";
    private static final int DEFAULT_CRITICAL_THRESHOLD = 120;

    @Transactional
    public Measurement save(Measurement measurement) {
        return measurementRepository.save(measurement);
    }

    @Transactional
    public List<Measurement> saveAll(List<Measurement> measurements) {
        return measurementRepository.saveAll(measurements);
    }

    @Transactional(readOnly = true)
    public Measurement getByStartTime(LocalDateTime startTime) {
        return measurementRepository.findByStartTime(startTime)
                .orElseThrow(() -> new NoSuchElementException(
                        "No measurement found for start time: " + startTime
                ));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDtoByStartTime(LocalDateTime startTime) {
        Measurement measurement = getByStartTime(startTime);
        return toDtoWithUnit(measurement);
    }

    @Transactional(readOnly = true)
    public boolean existsByStartTime(LocalDateTime startTime) {
        return measurementRepository.existsByStartTime(startTime);
    }

    @Transactional(readOnly = true)
    public List<Measurement> getMeasurementsBetween(LocalDateTime from, LocalDateTime to) {
        validateTimeRange(from, to);

        return measurementRepository
                .findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(from, to);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMeasurementDtosBetween(LocalDateTime from, LocalDateTime to) {
        return getMeasurementsBetween(from, to)
                .stream()
                .map(this::toDtoWithUnit)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countBetween(LocalDateTime from, LocalDateTime to) {
        validateTimeRange(from, to);

        return measurementRepository.countByStartTimeGreaterThanEqualAndStartTimeLessThan(from, to);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAverageBetween(LocalDateTime from, LocalDateTime to) {
        validateDataExists(from, to);

        Double average = measurementRepository.calculateAverageBetween(from, to);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("from", from);
        response.put("to", to);
        response.put("average", average);
        response.put("unit", UNIT);

        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMinBetween(LocalDateTime from, LocalDateTime to) {
        validateDataExists(from, to);

        Measurement minMeasurement = measurementRepository
                .findTopByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByOzoneAsc(from, to)
                .orElseThrow(() -> new NoSuchElementException("No minimum measurement found"));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("from", from);
        response.put("to", to);
        response.put("minValue", minMeasurement.getOzone());
        response.put("timestamp", minMeasurement.getStartTime());
        response.put("unit", UNIT);

        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMaxBetween(LocalDateTime from, LocalDateTime to) {
        validateDataExists(from, to);

        Measurement maxMeasurement = measurementRepository
                .findTopByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByOzoneDesc(from, to)
                .orElseThrow(() -> new NoSuchElementException("No maximum measurement found"));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("from", from);
        response.put("to", to);
        response.put("maxValue", maxMeasurement.getOzone());
        response.put("timestamp", maxMeasurement.getStartTime());
        response.put("unit", UNIT);

        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRangeBetween(LocalDateTime from, LocalDateTime to) {
        validateDataExists(from, to);

        Integer min = measurementRepository.findMinOzoneBetween(from, to);
        Integer max = measurementRepository.findMaxOzoneBetween(from, to);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("from", from);
        response.put("to", to);
        response.put("minValue", min);
        response.put("maxValue", max);
        response.put("range", max - min);
        response.put("unit", UNIT);

        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTop10Between(LocalDateTime from, LocalDateTime to) {
        validateTimeRange(from, to);

        return measurementRepository
                .findTop10ByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByOzoneDesc(from, to)
                .stream()
                .map(this::toDtoWithUnit)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopValuesBetween(
            LocalDateTime from,
            LocalDateTime to,
            int limit
    ) {
        validateTimeRange(from, to);

        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than 0");
        }

        return measurementRepository
                .findTopValuesBetween(from, to, PageRequest.of(0, limit))
                .stream()
                .map(this::toDtoWithUnit)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCriticalMeasurementsBetween(
            LocalDateTime from,
            LocalDateTime to
    ) {
        return getCriticalMeasurementsBetween(from, to, DEFAULT_CRITICAL_THRESHOLD);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCriticalMeasurementsBetween(
            LocalDateTime from,
            LocalDateTime to,
            int threshold
    ) {
        validateTimeRange(from, to);

        return measurementRepository
                .findByOzoneGreaterThanEqualAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
                        threshold,
                        from,
                        to
                )
                .stream()
                .map(measurement -> {
                    Map<String, Object> dto = toDtoWithUnit(measurement);
                    dto.put("threshold", threshold);
                    dto.put("critical", true);
                    return dto;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStatisticsReport(LocalDateTime from, LocalDateTime to) {
        validateDataExists(from, to);

        Double average = measurementRepository.calculateAverageBetween(from, to);
        Integer min = measurementRepository.findMinOzoneBetween(from, to);
        Integer max = measurementRepository.findMaxOzoneBetween(from, to);
        long measurementCount = measurementRepository.countByStartTimeGreaterThanEqualAndStartTimeLessThan(from, to);

        long criticalMeasurements = measurementRepository
                .findByOzoneGreaterThanEqualAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
                        DEFAULT_CRITICAL_THRESHOLD,
                        from,
                        to
                )
                .size();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("from", from);
        response.put("to", to);
        response.put("average", average);
        response.put("minValue", min);
        response.put("maxValue", max);
        response.put("range", max - min);
        response.put("measurementCount", measurementCount);
        response.put("criticalMeasurements", criticalMeasurements);
        response.put("threshold", DEFAULT_CRITICAL_THRESHOLD);
        response.put("unit", UNIT);

        return response;
    }

    private void validateDataExists(LocalDateTime from, LocalDateTime to) {
        validateTimeRange(from, to);

        long count = measurementRepository.countByStartTimeGreaterThanEqualAndStartTimeLessThan(from, to);

        if (count == 0) {
            throw new NoSuchElementException(
                    "No measurements found between " + from + " and " + to
            );
        }
    }

    private void validateTimeRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("From and to must not be null");
        }

        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("From must be before to");
        }
    }

    private Map<String, Object> toDtoWithUnit(Measurement measurement) {
        Map<String, Object> dto = new LinkedHashMap<>(measurement.toDto());
        dto.put("unit", UNIT);
        return dto;
    }
}
