package at.kaindorf.climate_project.pojo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StationBuilderTest {

    @Test
    void builderCreatesStation() {
        Station station = Station.builder()
                .id(7)
                .build();

        assertEquals(7, station.getId());
    }

    @Test
    void builderRejectsMissingId() {
        assertThrows(IllegalStateException.class, () -> Station.builder().build());
    }
}
