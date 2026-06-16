package at.kaindorf.climate_project.service;

import at.kaindorf.climate_project.pojo.Measurement;
import at.kaindorf.climate_project.repositories.MeasurementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MeasurementService {

    private static final String UNIT = "\u00b5g/m\u00b3";

    private final MeasurementRepository measurementRepository;

    @Transactional
    public List<Measurement> saveAll(List<Measurement> measurements) {
        return measurementRepository.saveAll(measurements);
    }

    @Transactional(readOnly = true)
    public long countAll() {
        return measurementRepository.count();
    }

    @Transactional(readOnly = true)
    public Measurement getByStartTime(LocalDateTime startTime, Integer stationId) {
        return findByStartTime(startTime, stationId)
                .orElseThrow(() -> new NoSuchElementException("No measurement found for start time: " + startTime));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDtoByStartTime(LocalDateTime startTime, Integer stationId) {
        return toDtoWithUnit(getByStartTime(startTime, stationId));
    }

    @Transactional(readOnly = true)
    public List<Measurement> getMeasurementsBetween(LocalDateTime from, LocalDateTime to, Integer stationId) {
        validateTimeRange(from, to);

        if (stationId == null) {
            return measurementRepository
                    .findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(from, to);
        }

        return measurementRepository
                .findByStation_IdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
                        stationId,
                        from,
                        to
                );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMeasurementDtosBetween(
            LocalDateTime from,
            LocalDateTime to,
            Integer stationId
    ) {
        return getMeasurementsBetween(from, to, stationId)
                .stream()
                .map(this::toDtoWithUnit)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAverageBetween(LocalDateTime from, LocalDateTime to, Integer stationId) {
        validateDataExists(from, to, stationId);

        Double average = stationId == null
                ? measurementRepository.calculateAverageBetween(from, to)
                : measurementRepository.calculateAverageBetween(stationId, from, to);

        Map<String, Object> response = baseRangeResponse(from, to, stationId);
        response.put("average", round(average));
        response.put("unit", UNIT);
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMinBetween(LocalDateTime from, LocalDateTime to, Integer stationId) {
        validateDataExists(from, to, stationId);

        Measurement minMeasurement = findMinMeasurement(from, to, stationId)
                .orElseThrow(() -> new NoSuchElementException("No minimum measurement found"));

        Map<String, Object> response = baseRangeResponse(from, to, stationId);
        response.put("minValue", minMeasurement.getOzone());
        response.put("timestamp", minMeasurement.getStartTime());
        response.put("unit", UNIT);
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMaxBetween(LocalDateTime from, LocalDateTime to, Integer stationId) {
        validateDataExists(from, to, stationId);

        Measurement maxMeasurement = findMaxMeasurement(from, to, stationId)
                .orElseThrow(() -> new NoSuchElementException("No maximum measurement found"));

        Map<String, Object> response = baseRangeResponse(from, to, stationId);
        response.put("maxValue", maxMeasurement.getOzone());
        response.put("timestamp", maxMeasurement.getStartTime());
        response.put("unit", UNIT);
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCriticalMeasurementsBetween(
            LocalDateTime from,
            LocalDateTime to,
            int threshold,
            Integer stationId
    ) {
        validateTimeRange(from, to);

        if (threshold <= 0) {
            throw new IllegalArgumentException("Threshold must be greater than 0");
        }

        List<Measurement> measurements = stationId == null
                ? measurementRepository
                        .findByOzoneGreaterThanEqualAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
                                BigDecimal.valueOf(threshold),
                                from,
                                to
                        )
                : measurementRepository
                        .findByStation_IdAndOzoneGreaterThanEqualAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
                                stationId,
                                BigDecimal.valueOf(threshold),
                                from,
                                to
                        );

        return measurements
                .stream()
                .map(measurement -> {
                    Map<String, Object> dto = toDtoWithUnit(measurement);
                    dto.put("threshold", threshold);
                    dto.put("critical", true);
                    return dto;
                })
                .toList();
    }

    private Optional<Measurement> findByStartTime(LocalDateTime startTime, Integer stationId) {
        if (stationId == null) {
            return measurementRepository.findFirstByStartTimeOrderByIdAsc(startTime);
        }

        return measurementRepository.findFirstByStation_IdAndStartTimeOrderByIdAsc(stationId, startTime);
    }

    private Optional<Measurement> findMinMeasurement(LocalDateTime from, LocalDateTime to, Integer stationId) {
        if (stationId == null) {
            return measurementRepository.findTopByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByOzoneAsc(from, to);
        }

        return measurementRepository.findTopByStation_IdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByOzoneAsc(
                stationId,
                from,
                to
        );
    }

    private Optional<Measurement> findMaxMeasurement(LocalDateTime from, LocalDateTime to, Integer stationId) {
        if (stationId == null) {
            return measurementRepository.findTopByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByOzoneDesc(from, to);
        }

        return measurementRepository.findTopByStation_IdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByOzoneDesc(
                stationId,
                from,
                to
        );
    }

    private BigDecimal round(Double value) {
        if (value == null) {
            return null;
        }

        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private void validateDataExists(LocalDateTime from, LocalDateTime to, Integer stationId) {
        if (getMeasurementsBetween(from, to, stationId).isEmpty()) {
            throw new NoSuchElementException("No measurements found between " + from + " and " + to);
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

    private Map<String, Object> baseRangeResponse(LocalDateTime from, LocalDateTime to, Integer stationId) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("from", from);
        response.put("to", to);

        if (stationId != null) {
            response.put("stationId", stationId);
        }

        return response;
    }

    private Map<String, Object> toDtoWithUnit(Measurement measurement) {
        Map<String, Object> dto = new LinkedHashMap<>(measurement.toDto());
        if (measurement.getStation() != null) {
            dto.put("stationId", measurement.getStation().getId());
        }
        dto.put("unit", UNIT);
        return dto;
    }
}
