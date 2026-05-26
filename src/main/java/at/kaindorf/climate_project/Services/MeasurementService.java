package at.kaindorf.climate_project.Services;

import at.kaindorf.climate_project.pojo.Measurement;
import at.kaindorf.climate_project.repositories.MeasurementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeasurementService {

    private static final String UNIT = "\u00b5g/m\u00b3";
    private static final int DEFAULT_CRITICAL_THRESHOLD = 120;
    private static final int ROLLING_WINDOW_HOURS = 8;

    private final MeasurementRepository measurementRepository;

    @Transactional
    public Measurement save(Measurement measurement) {
        return measurementRepository.save(measurement);
    }

    @Transactional
    public List<Measurement> saveAll(List<Measurement> measurements) {
        return measurementRepository.saveAll(measurements);
    }

    @Transactional(readOnly = true)
    public boolean existsByStartTime(LocalDateTime startTime) {
        return measurementRepository.existsByStartTime(startTime);
    }

    @Transactional(readOnly = true)
    public boolean existsByStationIdAndStartTime(Integer stationId, LocalDateTime startTime) {
        return measurementRepository.existsByStation_IdAndStartTime(stationId, startTime);
    }

    @Transactional(readOnly = true)
    public long countAll() {
        return measurementRepository.count();
    }

    @Transactional(readOnly = true)
    public Measurement getByStartTime(LocalDateTime startTime) {
        return getByStartTime(startTime, null);
    }

    @Transactional(readOnly = true)
    public Measurement getByStartTime(LocalDateTime startTime, Integer stationId) {
        return findByStartTime(startTime, stationId)
                .orElseThrow(() -> new NoSuchElementException("No measurement found for start time: " + startTime));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDtoByStartTime(LocalDateTime startTime) {
        return getDtoByStartTime(startTime, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDtoByStartTime(LocalDateTime startTime, Integer stationId) {
        return toDtoWithUnit(getByStartTime(startTime, stationId));
    }

    @Transactional(readOnly = true)
    public List<Measurement> getMeasurementsBetween(LocalDateTime from, LocalDateTime to) {
        return getMeasurementsBetween(from, to, null);
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
    public List<Map<String, Object>> getMeasurementDtosBetween(LocalDateTime from, LocalDateTime to) {
        return getMeasurementDtosBetween(from, to, null);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMeasurementDtosBetween(LocalDateTime from, LocalDateTime to, Integer stationId) {
        return getMeasurementsBetween(from, to, stationId)
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
        return getAverageBetween(from, to, null);
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
    public Map<String, Object> getMinBetween(LocalDateTime from, LocalDateTime to) {
        return getMinBetween(from, to, null);
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
    public Map<String, Object> getMaxBetween(LocalDateTime from, LocalDateTime to) {
        return getMaxBetween(from, to, null);
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
    public Map<String, Object> getRangeBetween(LocalDateTime from, LocalDateTime to) {
        return getRangeBetween(from, to, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRangeBetween(LocalDateTime from, LocalDateTime to, Integer stationId) {
        validateDataExists(from, to, stationId);

        BigDecimal min = findMinValue(from, to, stationId);
        BigDecimal max = findMaxValue(from, to, stationId);

        Map<String, Object> response = baseRangeResponse(from, to, stationId);
        response.put("minValue", min);
        response.put("maxValue", max);
        response.put("range", max.subtract(min));
        response.put("unit", UNIT);
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTop10Between(LocalDateTime from, LocalDateTime to) {
        return getTopValuesBetween(from, to, 10, null);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopValuesBetween(LocalDateTime from, LocalDateTime to, int limit) {
        return getTopValuesBetween(from, to, limit, null);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopValuesBetween(
            LocalDateTime from,
            LocalDateTime to,
            int limit,
            Integer stationId
    ) {
        validateTimeRange(from, to);

        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than 0");
        }

        return measurementRepository
                .findTopValuesBetween(stationId, from, to, PageRequest.of(0, limit))
                .stream()
                .map(this::toDtoWithUnit)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCriticalMeasurementsBetween(LocalDateTime from, LocalDateTime to) {
        return getCriticalMeasurementsBetween(from, to, DEFAULT_CRITICAL_THRESHOLD, null);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCriticalMeasurementsBetween(
            LocalDateTime from,
            LocalDateTime to,
            int threshold
    ) {
        return getCriticalMeasurementsBetween(from, to, threshold, null);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCriticalMeasurementsBetween(
            LocalDateTime from,
            LocalDateTime to,
            int threshold,
            Integer stationId
    ) {
        validateTimeRange(from, to);

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

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAverageByDay(LocalDateTime from, LocalDateTime to, Integer stationId) {
        return groupValues(from, to, stationId, measurement -> measurement.getStartTime().toLocalDate().toString(), "day", "average");
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAverageByWeek(LocalDateTime from, LocalDateTime to, Integer stationId) {
        WeekFields weekFields = WeekFields.ISO;
        return groupValues(
                from,
                to,
                stationId,
                measurement -> {
                    LocalDate date = measurement.getStartTime().toLocalDate();
                    int week = date.get(weekFields.weekOfWeekBasedYear());
                    int year = date.get(weekFields.weekBasedYear());
                    return year + "-W" + String.format("%02d", week);
                },
                "week",
                "average"
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAverageByMonth(LocalDateTime from, LocalDateTime to, Integer stationId) {
        return groupValues(
                from,
                to,
                stationId,
                measurement -> YearMonth.from(measurement.getStartTime()).toString(),
                "month",
                "average"
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAverageByYear(LocalDateTime from, LocalDateTime to, Integer stationId) {
        return groupValues(
                from,
                to,
                stationId,
                measurement -> String.valueOf(measurement.getStartTime().getYear()),
                "year",
                "average"
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMaxByDay(LocalDateTime from, LocalDateTime to, Integer stationId) {
        return groupExtreme(from, to, stationId, measurement -> measurement.getStartTime().toLocalDate().toString(), "day", "maxValue", true);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMaxByWeek(LocalDateTime from, LocalDateTime to, Integer stationId) {
        WeekFields weekFields = WeekFields.ISO;
        return groupExtreme(
                from,
                to,
                stationId,
                measurement -> {
                    LocalDate date = measurement.getStartTime().toLocalDate();
                    return date.get(weekFields.weekBasedYear()) + "-W" + String.format("%02d", date.get(weekFields.weekOfWeekBasedYear()));
                },
                "week",
                "maxValue",
                true
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMaxByMonth(LocalDateTime from, LocalDateTime to, Integer stationId) {
        return groupExtreme(from, to, stationId, measurement -> YearMonth.from(measurement.getStartTime()).toString(), "month", "maxValue", true);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMaxByYear(LocalDateTime from, LocalDateTime to, Integer stationId) {
        return groupExtreme(from, to, stationId, measurement -> String.valueOf(measurement.getStartTime().getYear()), "year", "maxValue", true);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMinByDay(LocalDateTime from, LocalDateTime to, Integer stationId) {
        return groupExtreme(from, to, stationId, measurement -> measurement.getStartTime().toLocalDate().toString(), "day", "minValue", false);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMinByWeek(LocalDateTime from, LocalDateTime to, Integer stationId) {
        WeekFields weekFields = WeekFields.ISO;
        return groupExtreme(
                from,
                to,
                stationId,
                measurement -> {
                    LocalDate date = measurement.getStartTime().toLocalDate();
                    return date.get(weekFields.weekBasedYear()) + "-W" + String.format("%02d", date.get(weekFields.weekOfWeekBasedYear()));
                },
                "week",
                "minValue",
                false
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMinByMonth(LocalDateTime from, LocalDateTime to, Integer stationId) {
        return groupExtreme(from, to, stationId, measurement -> YearMonth.from(measurement.getStartTime()).toString(), "month", "minValue", false);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMinByYear(LocalDateTime from, LocalDateTime to, Integer stationId) {
        return groupExtreme(from, to, stationId, measurement -> String.valueOf(measurement.getStartTime().getYear()), "year", "minValue", false);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMonthRanking(int year, Integer stationId) {
        LocalDateTime from = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime to = from.plusYears(1);
        validateDataExists(from, to, stationId);

        return getMeasurementsBetween(from, to, stationId)
                .stream()
                .collect(Collectors.groupingBy(
                        measurement -> measurement.getStartTime().getMonth(),
                        TreeMap::new,
                        Collectors.toList()
                ))
                .entrySet()
                .stream()
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    Month month = entry.getKey();
                    item.put("year", year);
                    item.put("month", month.getValue());
                    item.put("monthName", month.name());
                    item.put("average", average(entry.getValue()));
                    item.put("unit", UNIT);
                    if (stationId != null) {
                        item.put("stationId", stationId);
                    }
                    return item;
                })
                .sorted((left, right) -> ((BigDecimal) right.get("average")).compareTo((BigDecimal) left.get("average")))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getYearComparison(int fromYear, int toYear, Integer stationId) {
        if (fromYear > toYear) {
            throw new IllegalArgumentException("fromYear must be before or equal to toYear");
        }

        List<Map<String, Object>> response = new ArrayList<>();

        for (int year = fromYear; year <= toYear; year++) {
            LocalDateTime from = LocalDate.of(year, 1, 1).atStartOfDay();
            LocalDateTime to = from.plusYears(1);
            List<Measurement> measurements = getMeasurementsBetween(from, to, stationId);

            if (measurements.isEmpty()) {
                continue;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("year", year);
            if (stationId != null) {
                item.put("stationId", stationId);
            }
            item.put("average", average(measurements));
            item.put("minValue", min(measurements));
            item.put("maxValue", max(measurements));
            item.put("criticalDays", calculateCriticalDays(measurements, DEFAULT_CRITICAL_THRESHOLD).size());
            item.put("unit", UNIT);
            response.add(item);
        }

        if (response.isEmpty()) {
            throw new NoSuchElementException("No measurements found between years " + fromYear + " and " + toYear);
        }

        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCriticalDays(LocalDateTime from, LocalDateTime to, Integer stationId) {
        validateDataExists(from, to, stationId);
        List<Measurement> measurements = getMeasurementsBetween(from, to, stationId);
        List<Map<String, Object>> days = calculateCriticalDays(measurements, DEFAULT_CRITICAL_THRESHOLD);

        Map<String, Object> response = baseRangeResponse(from, to, stationId);
        response.put("threshold", DEFAULT_CRITICAL_THRESHOLD);
        response.put("criticalDays", days.size());
        response.put("days", days);
        response.put("unit", UNIT);
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLongestCriticalPeriod(LocalDateTime from, LocalDateTime to, Integer stationId) {
        validateDataExists(from, to, stationId);
        List<RollingAverage> criticalWindows = calculateRollingAverages(getMeasurementsBetween(from, to, stationId))
                .stream()
                .filter(window -> window.average().compareTo(BigDecimal.valueOf(DEFAULT_CRITICAL_THRESHOLD)) > 0)
                .sorted(Comparator.comparing(RollingAverage::stationId).thenComparing(RollingAverage::startTime))
                .toList();

        CriticalPeriod longest = null;
        CriticalPeriod current = null;

        for (RollingAverage window : criticalWindows) {
            if (current == null || !current.canContinueWith(window)) {
                current = new CriticalPeriod(window);
            } else {
                current = current.extend(window);
            }

            if (longest == null || current.hours() > longest.hours()) {
                longest = current;
            }
        }

        Map<String, Object> response = baseRangeResponse(from, to, stationId);
        response.put("threshold", DEFAULT_CRITICAL_THRESHOLD);

        if (longest == null) {
            response.put("longestCriticalPeriod", null);
            return response;
        }

        Map<String, Object> period = new LinkedHashMap<>();
        period.put("stationId", longest.stationId());
        period.put("startTime", longest.startTime());
        period.put("endTime", longest.endTime());
        period.put("hours", longest.hours());
        period.put("max8HourAverage", longest.maxAverage());
        period.put("unit", UNIT);
        response.put("longestCriticalPeriod", period);
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTrend(LocalDateTime from, LocalDateTime to, Integer stationId) {
        validateDataExists(from, to, stationId);

        LocalDateTime middle = from.plus(Duration.between(from, to).dividedBy(2));
        List<Measurement> firstHalf = getMeasurementsBetween(from, middle, stationId);
        List<Measurement> secondHalf = getMeasurementsBetween(middle, to, stationId);

        if (firstHalf.isEmpty() || secondHalf.isEmpty()) {
            throw new NoSuchElementException("Not enough measurements to calculate trend");
        }

        BigDecimal firstAverage = average(firstHalf);
        BigDecimal secondAverage = average(secondHalf);
        BigDecimal difference = secondAverage.subtract(firstAverage);

        String trend;
        if (difference.abs().compareTo(BigDecimal.ONE) <= 0) {
            trend = "stable";
        } else if (difference.compareTo(BigDecimal.ZERO) > 0) {
            trend = "increasing";
        } else {
            trend = "decreasing";
        }

        Map<String, Object> response = baseRangeResponse(from, to, stationId);
        response.put("firstHalfAverage", firstAverage);
        response.put("secondHalfAverage", secondAverage);
        response.put("difference", difference);
        response.put("trend", trend);
        response.put("unit", UNIT);
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStatisticsReport(LocalDateTime from, LocalDateTime to) {
        return getStatisticsReport(from, to, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStatisticsReport(LocalDateTime from, LocalDateTime to, Integer stationId) {
        validateDataExists(from, to, stationId);

        List<Measurement> measurements = getMeasurementsBetween(from, to, stationId);
        BigDecimal min = findMinValue(from, to, stationId);
        BigDecimal max = findMaxValue(from, to, stationId);
        BigDecimal highest8HourAverage = calculateRollingAverages(measurements)
                .stream()
                .map(RollingAverage::average)
                .max(BigDecimal::compareTo)
                .orElse(null);

        Map<String, Object> response = baseRangeResponse(from, to, stationId);
        response.put("average", average(measurements));
        response.put("minValue", min);
        response.put("maxValue", max);
        response.put("range", max.subtract(min));
        response.put("measurementCount", measurements.size());
        response.put("criticalDays", calculateCriticalDays(measurements, DEFAULT_CRITICAL_THRESHOLD).size());
        response.put("highest8HourAverage", highest8HourAverage);
        response.put("trend", getTrend(from, to, stationId).get("trend"));
        response.put("threshold", DEFAULT_CRITICAL_THRESHOLD);
        response.put("unit", UNIT);
        return response;
    }

    private java.util.Optional<Measurement> findByStartTime(LocalDateTime startTime, Integer stationId) {
        if (stationId == null) {
            return measurementRepository.findFirstByStartTimeOrderByIdAsc(startTime);
        }

        return measurementRepository.findFirstByStation_IdAndStartTimeOrderByIdAsc(stationId, startTime);
    }

    private java.util.Optional<Measurement> findMinMeasurement(LocalDateTime from, LocalDateTime to, Integer stationId) {
        if (stationId == null) {
            return measurementRepository.findTopByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByOzoneAsc(from, to);
        }

        return measurementRepository.findTopByStation_IdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByOzoneAsc(
                stationId,
                from,
                to
        );
    }

    private java.util.Optional<Measurement> findMaxMeasurement(LocalDateTime from, LocalDateTime to, Integer stationId) {
        if (stationId == null) {
            return measurementRepository.findTopByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByOzoneDesc(from, to);
        }

        return measurementRepository.findTopByStation_IdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByOzoneDesc(
                stationId,
                from,
                to
        );
    }

    private BigDecimal findMinValue(LocalDateTime from, LocalDateTime to, Integer stationId) {
        if (stationId == null) {
            return measurementRepository.findMinOzoneBetween(from, to);
        }

        return measurementRepository.findMinOzoneBetween(stationId, from, to);
    }

    private BigDecimal findMaxValue(LocalDateTime from, LocalDateTime to, Integer stationId) {
        if (stationId == null) {
            return measurementRepository.findMaxOzoneBetween(from, to);
        }

        return measurementRepository.findMaxOzoneBetween(stationId, from, to);
    }

    private List<Map<String, Object>> groupValues(
            LocalDateTime from,
            LocalDateTime to,
            Integer stationId,
            Function<Measurement, String> keyMapper,
            String keyName,
            String valueName
    ) {
        validateDataExists(from, to, stationId);

        return getMeasurementsBetween(from, to, stationId)
                .stream()
                .collect(Collectors.groupingBy(keyMapper, TreeMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put(keyName, entry.getKey());
                    item.put(valueName, average(entry.getValue()));
                    item.put("unit", UNIT);
                    if (stationId != null) {
                        item.put("stationId", stationId);
                    }
                    return item;
                })
                .toList();
    }

    private List<Map<String, Object>> groupExtreme(
            LocalDateTime from,
            LocalDateTime to,
            Integer stationId,
            Function<Measurement, String> keyMapper,
            String keyName,
            String valueName,
            boolean maximum
    ) {
        validateDataExists(from, to, stationId);

        return getMeasurementsBetween(from, to, stationId)
                .stream()
                .collect(Collectors.groupingBy(keyMapper, TreeMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<Measurement> values = entry.getValue();
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put(keyName, entry.getKey());
                    item.put(valueName, maximum ? max(values) : min(values));
                    item.put("unit", UNIT);
                    if (stationId != null) {
                        item.put("stationId", stationId);
                    }
                    return item;
                })
                .toList();
    }

    private List<Map<String, Object>> calculateCriticalDays(List<Measurement> measurements, int threshold) {
        Map<String, BigDecimal> maxByStationAndDay = new TreeMap<>();

        for (RollingAverage window : calculateRollingAverages(measurements)) {
            LocalDate day = window.endTime().toLocalDate();
            String key = window.stationId() + "|" + day;
            maxByStationAndDay.merge(key, window.average(), (left, right) -> left.compareTo(right) >= 0 ? left : right);
        }

        return maxByStationAndDay
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().compareTo(BigDecimal.valueOf(threshold)) > 0)
                .map(entry -> {
                    String[] keyParts = entry.getKey().split("\\|");
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("stationId", Integer.parseInt(keyParts[0]));
                    item.put("date", keyParts[1]);
                    item.put("max8HourAverage", entry.getValue());
                    item.put("threshold", threshold);
                    item.put("unit", UNIT);
                    return item;
                })
                .toList();
    }

    private List<RollingAverage> calculateRollingAverages(List<Measurement> measurements) {
        Map<Integer, List<Measurement>> byStation = measurements
                .stream()
                .filter(measurement -> measurement.getStation() != null)
                .collect(Collectors.groupingBy(measurement -> measurement.getStation().getId()));

        List<RollingAverage> rollingAverages = new ArrayList<>();

        for (Map.Entry<Integer, List<Measurement>> entry : byStation.entrySet()) {
            List<Measurement> stationMeasurements = entry.getValue()
                    .stream()
                    .sorted(Comparator.comparing(Measurement::getStartTime))
                    .toList();

            for (int index = 0; index <= stationMeasurements.size() - ROLLING_WINDOW_HOURS; index++) {
                List<Measurement> window = stationMeasurements.subList(index, index + ROLLING_WINDOW_HOURS);

                if (!isContinuousHourlyWindow(window)) {
                    continue;
                }

                BigDecimal average = average(window);
                LocalDateTime startTime = window.get(0).getStartTime();
                LocalDateTime endTime = window.get(window.size() - 1).getEndTime();
                rollingAverages.add(new RollingAverage(entry.getKey(), startTime, endTime, average));
            }
        }

        return rollingAverages;
    }

    private boolean isContinuousHourlyWindow(List<Measurement> window) {
        for (int index = 1; index < window.size(); index++) {
            LocalDateTime expected = window.get(index - 1).getStartTime().plusHours(1);

            if (!expected.equals(window.get(index).getStartTime())) {
                return false;
            }
        }

        return true;
    }

    private BigDecimal average(List<Measurement> measurements) {
        BigDecimal sum = measurements
                .stream()
                .map(Measurement::getOzone)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(measurements.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal min(List<Measurement> measurements) {
        return measurements.stream().map(Measurement::getOzone).min(BigDecimal::compareTo).orElseThrow();
    }

    private BigDecimal max(List<Measurement> measurements) {
        return measurements.stream().map(Measurement::getOzone).max(BigDecimal::compareTo).orElseThrow();
    }

    private BigDecimal round(Double value) {
        if (value == null) {
            return null;
        }

        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private void validateDataExists(LocalDateTime from, LocalDateTime to, Integer stationId) {
        List<Measurement> measurements = getMeasurementsBetween(from, to, stationId);

        if (measurements.isEmpty()) {
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

    private static class RollingAverage {
        private Integer stationId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private BigDecimal average;

        RollingAverage(Integer stationId, LocalDateTime startTime, LocalDateTime endTime, BigDecimal average) {
            this.stationId = stationId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.average = average;
        }

        Integer stationId() {
            return stationId;
        }

        LocalDateTime startTime() {
            return startTime;
        }

        LocalDateTime endTime() {
            return endTime;
        }

        BigDecimal average() {
            return average;
        }
    }

    private static class CriticalPeriod {
        private Integer stationId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int hours;
        private BigDecimal maxAverage;

        CriticalPeriod(RollingAverage window) {
            this(
                    window.stationId(),
                    window.startTime(),
                    window.endTime(),
                    (int) Duration.between(window.startTime(), window.endTime()).toHours(),
                    window.average()
            );
        }

        CriticalPeriod(
                Integer stationId,
                LocalDateTime startTime,
                LocalDateTime endTime,
                int hours,
                BigDecimal maxAverage
        ) {
            this.stationId = stationId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.hours = hours;
            this.maxAverage = maxAverage;
        }

        Integer stationId() {
            return stationId;
        }

        LocalDateTime startTime() {
            return startTime;
        }

        LocalDateTime endTime() {
            return endTime;
        }

        int hours() {
            return hours;
        }

        BigDecimal maxAverage() {
            return maxAverage;
        }

        boolean canContinueWith(RollingAverage window) {
            return stationId.equals(window.stationId()) && endTime.minusHours(ROLLING_WINDOW_HOURS - 1).equals(window.startTime());
        }

        CriticalPeriod extend(RollingAverage window) {
            BigDecimal newMaxAverage = maxAverage.compareTo(window.average()) >= 0 ? maxAverage : window.average();
            return new CriticalPeriod(
                    stationId,
                    startTime,
                    window.endTime(),
                    (int) Duration.between(startTime, window.endTime()).toHours(),
                    newMaxAverage
            );
        }
    }
}
