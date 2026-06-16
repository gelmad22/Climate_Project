package at.kaindorf.climate_project.pojo;

import at.kaindorf.climate_project.annotations.ToDto;
import at.kaindorf.climate_project.mapper.DtoMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(
        name = "ozone_measurements",
        uniqueConstraints = @UniqueConstraint(columnNames = {"station_id", "start_time"}),
        indexes = {
                @Index(columnList = "station_id,start_time"),
                @Index(columnList = "station_id,value_ug_m3")
        }
)
@Data
@NoArgsConstructor
public class Measurement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @ToDto
    @Column(name = "value_ug_m3", nullable = false, precision = 8, scale = 2)
    private BigDecimal ozone;

    @ToDto
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @ToDto
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Map<String, Object> toDto() {
        return DtoMapper.map(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Station station;
        private BigDecimal ozone;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private LocalDateTime createdAt;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder station(Station station) {
            this.station = station;
            return this;
        }

        public Builder ozone(BigDecimal ozone) {
            this.ozone = ozone;
            return this;
        }

        public Builder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Measurement build() {
            validate();

            Measurement measurement = new Measurement();
            measurement.setId(id);
            measurement.setStation(station);
            measurement.setOzone(ozone);
            measurement.setStartTime(startTime);
            measurement.setEndTime(endTime);
            measurement.setCreatedAt(createdAt);
            return measurement;
        }

        private void validate() {
            if (station == null) {
                throw new IllegalStateException("Measurement station must not be null");
            }

            if (ozone == null) {
                throw new IllegalStateException("Measurement ozone must not be null");
            }

            if (startTime == null) {
                throw new IllegalStateException("Measurement start time must not be null");
            }

            if (endTime == null) {
                throw new IllegalStateException("Measurement end time must not be null");
            }

            if (!startTime.isBefore(endTime)) {
                throw new IllegalStateException("Measurement start time must be before end time");
            }
        }
    }
}
