package at.kaindorf.climate_project.db;

import at.kaindorf.climate_project.Services.MeasurementService;
import at.kaindorf.climate_project.pojo.Measurement;
import at.kaindorf.climate_project.pojo.Station;
import at.kaindorf.climate_project.repositories.StationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

@Component
public class InitDb implements CommandLineRunner {

    private MeasurementService measurementService;
    private StationRepository stationRepository;
    private ResourceLoader resourceLoader;
    private ObjectMapper objectMapper;
    private String importFilePath;

    public InitDb(
            MeasurementService measurementService,
            StationRepository stationRepository,
            ResourceLoader resourceLoader,
            @Value("${climate.ozone.import.file:classpath:data.json}") String importFilePath
    ) {
        this.measurementService = measurementService;
        this.stationRepository = stationRepository;
        this.resourceLoader = resourceLoader;
        this.objectMapper = JsonMapper.builder().build();
        this.importFilePath = importFilePath;
    }

    @Override
    public void run(String... args) {
        if (importFilePath == null || importFilePath.isBlank()) {
            return;
        }

        if (measurementService.countAll() > 0) {
            return;
        }

        try {
            importOzoneMeasurements();
        } catch (Exception e) {
            throw new IllegalStateException("Could not import ozone measurements from " + importFilePath, e);
        }
    }

    private void importOzoneMeasurements() throws Exception {
        Resource importResource = resourceLoader.getResource(importFilePath);

        if (!importResource.exists()) {
            throw new IllegalStateException("Configured ozone import file does not exist: " + importFilePath);
        }

        JsonNode rootNode;

        try (InputStream inputStream = importResource.getInputStream()) {
            rootNode = objectMapper.readTree(inputStream);
        }

        JsonNode dataNode = rootNode.path("data");

        if (!dataNode.isObject()) {
            throw new IllegalArgumentException("JSON does not contain an object field named data");
        }

        for (Map.Entry<String, JsonNode> stationEntry : dataNode.properties()) {
            Integer stationId = Integer.parseInt(stationEntry.getKey());
            JsonNode stationNode = stationEntry.getValue();

            if (!stationNode.isObject()) {
                continue;
            }

            Station station = stationRepository.findById(stationId)
                    .orElseGet(() -> stationRepository.save(Station.builder().id(stationId).build()));

            importStationMeasurements(station, stationNode);
        }
    }

    private void importStationMeasurements(Station station, JsonNode stationNode) {
        List<Measurement> batch = new ArrayList<>(1000);

        for (Map.Entry<String, JsonNode> entry : stationNode.properties()) {
            Measurement measurement;

            try {
                measurement = parseMeasurement(station, entry.getKey(), entry.getValue());
            } catch (IllegalArgumentException | DateTimeParseException e) {
                continue;
            }

            if (measurement == null) {
                continue;
            }

            batch.add(measurement);

            if (batch.size() >= 1000) {
                saveBatch(batch);
            }
        }

        saveBatch(batch);
    }

    private Measurement parseMeasurement(Station station, String startTimeText, JsonNode valuesNode) {
        if (!valuesNode.isArray() || valuesNode.size() < 5) {
            throw new IllegalArgumentException("measurement entry must be an array with 5 values");
        }

        int componentId = readInt(valuesNode.get(0), "component id");
        int scopeId = readInt(valuesNode.get(1), "scope id");

        if (componentId != 3 || scopeId != 2) {
            return null;
        }

        BigDecimal ozoneValue = readDecimal(valuesNode.get(2), "ozone value");
        String endTimeText = readText(valuesNode.get(3), "end time");

        return Measurement.builder()
                .station(station)
                .ozone(ozoneValue)
                .startTime(parseDateTime(startTimeText))
                .endTime(parseDateTime(endTimeText))
                .build();
    }

    private int readInt(JsonNode node, String fieldName) {
        if (node == null) {
            throw new IllegalArgumentException(fieldName + " must be an integer");
        }

        OptionalInt value = node.intValueOpt();

        if (value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must be an integer");
        }

        return value.getAsInt();
    }

    private String readText(JsonNode node, String fieldName) {
        if (node == null || !node.isString()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        Optional<String> value = node.stringValueOpt();

        if (value.isEmpty() || value.get().isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return value.get();
    }

    private BigDecimal readDecimal(JsonNode node, String fieldName) {
        if (node == null) {
            throw new IllegalArgumentException(fieldName + " must be a number");
        }

        Optional<BigDecimal> value = node.decimalValueOpt();

        if (value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must be a number");
        }

        return value.get();
    }

    private LocalDateTime parseDateTime(String value) {
        if (value.endsWith(" 24:00:00")) {
            LocalDate date = LocalDate.parse(value.substring(0, 10), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return date.plusDays(1).atStartOfDay();
        }

        return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private void saveBatch(List<Measurement> batch) {
        if (batch.isEmpty()) {
            return;
        }

        measurementService.saveAll(batch);
        batch.clear();
    }
}
