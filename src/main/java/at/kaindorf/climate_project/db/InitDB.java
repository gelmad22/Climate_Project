package at.kaindorf.climate_project.db;

import at.kaindorf.climate_project.Services.MeasurementService;
import at.kaindorf.climate_project.pojo.Measurement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
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
public class InitDB implements CommandLineRunner {

    private MeasurementService measurementService;
    private ObjectMapper objectMapper;
    private String importFilePath;

    public InitDB(
            MeasurementService measurementService,
            @Value("${climate.ozone.import.file:}") String importFilePath
    ) {
        this.measurementService = measurementService;
        this.objectMapper = JsonMapper.builder().build();
        this.importFilePath = importFilePath;
    }

    @Override
    public void run(String... args) {
        if (importFilePath == null || importFilePath.isBlank()) {
            return;
        }

        Path importPath = Path.of(importFilePath).toAbsolutePath().normalize();

        if (!Files.isRegularFile(importPath)) {
            throw new IllegalStateException("Configured ozone import file does not exist: " + importPath);
        }

        try {
            importOzoneMeasurements(importPath);
        } catch (Exception e) {
            throw new IllegalStateException("Could not import ozone measurements from " + importPath, e);
        }
    }

    private void importOzoneMeasurements(Path importPath) throws Exception {
        JsonNode rootNode = objectMapper.readTree(importPath);
        JsonNode dataNode = rootNode.path("data");

        if (!dataNode.isObject()) {
            throw new IllegalArgumentException("JSON does not contain an object field named data");
        }

        JsonNode stationNode = dataNode.get("1528");

        if (stationNode == null || !stationNode.isObject()) {
            throw new IllegalArgumentException("JSON does not contain data for station 1528");
        }

        importStationMeasurements(stationNode);
    }

    private void importStationMeasurements(JsonNode stationNode) {
        List<Measurement> batch = new ArrayList<>(1000);

        for (Map.Entry<String, JsonNode> entry : stationNode.properties()) {
            Measurement measurement;

            try {
                measurement = parseMeasurement(entry.getKey(), entry.getValue());
            } catch (IllegalArgumentException | DateTimeParseException e) {
                continue;
            }

            if (measurement == null) {
                continue;
            }

            if (measurementService.existsByStartTime(measurement.getStartTime())) {
                continue;
            }

            batch.add(measurement);

            if (batch.size() >= 1000) {
                saveBatch(batch);
            }
        }

        saveBatch(batch);
    }

    private Measurement parseMeasurement(String startTimeText, JsonNode valuesNode) {
        if (!valuesNode.isArray() || valuesNode.size() < 5) {
            throw new IllegalArgumentException("measurement entry must be an array with 5 values");
        }

        int componentId = readInt(valuesNode.get(0), "component id");
        int scopeId = readInt(valuesNode.get(1), "scope id");

        if (componentId != 3 || scopeId != 2) {
            return null;
        }

        int ozoneValue = readInt(valuesNode.get(2), "ozone value");
        String endTimeText = readText(valuesNode.get(3), "end time");

        Measurement measurement = new Measurement();
        measurement.setOzone(ozoneValue);
        measurement.setStartTime(parseDateTime(startTimeText));
        measurement.setEndTime(parseDateTime(endTimeText));
        return measurement;
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
