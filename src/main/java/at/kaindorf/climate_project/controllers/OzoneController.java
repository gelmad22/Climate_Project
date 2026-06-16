package at.kaindorf.climate_project.controllers;

import at.kaindorf.climate_project.service.MeasurementService;
import at.kaindorf.climate_project.web.OzoneDateTimeParser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ozone")
@RequiredArgsConstructor
public class OzoneController {

    private final MeasurementService measurementService;
    private final OzoneDateTimeParser dateTimeParser;

    @GetMapping("/measurements")
    public List<Map<String, Object>> getMeasurements(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getMeasurementDtosBetween(
                dateTimeParser.parse(from),
                dateTimeParser.parse(to),
                stationId
        );
    }

    @GetMapping("/measurement/{timestamp}")
    public Map<String, Object> getMeasurement(
            @PathVariable String timestamp,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getDtoByStartTime(dateTimeParser.parse(timestamp), stationId);
    }

    @GetMapping("/average")
    public Map<String, Object> getAverage(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getAverageBetween(
                dateTimeParser.parse(from),
                dateTimeParser.parse(to),
                stationId
        );
    }

    @GetMapping("/min")
    public Map<String, Object> getMin(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getMinBetween(
                dateTimeParser.parse(from),
                dateTimeParser.parse(to),
                stationId
        );
    }

    @GetMapping("/max")
    public Map<String, Object> getMax(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getMaxBetween(
                dateTimeParser.parse(from),
                dateTimeParser.parse(to),
                stationId
        );
    }

    @GetMapping("/critical")
    public List<Map<String, Object>> getCritical(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "120") int threshold,
            @RequestParam(required = false) Integer stationId
    ) {
        return measurementService.getCriticalMeasurementsBetween(
                dateTimeParser.parse(from),
                dateTimeParser.parse(to),
                threshold,
                stationId
        );
    }
}
