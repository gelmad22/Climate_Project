package at.kaindorf.climate_project.repositories;

import at.kaindorf.climate_project.pojo.Measurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeasurementRepository extends JpaRepository<Measurement, Long> {

    Optional<Measurement> findFirstByStartTimeOrderByIdAsc(LocalDateTime startTime);

    Optional<Measurement> findFirstByStation_IdAndStartTimeOrderByIdAsc(Integer stationId, LocalDateTime startTime);

    List<Measurement> findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
            LocalDateTime from,
            LocalDateTime to
    );

    List<Measurement> findByStation_IdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
            Integer stationId,
            LocalDateTime from,
            LocalDateTime to
    );

    @Query("""
        SELECT AVG(m.ozone)
        FROM Measurement m
        WHERE m.startTime >= :from
        AND m.startTime < :to
    """)
    Double calculateAverageBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
        SELECT AVG(m.ozone)
        FROM Measurement m
        WHERE m.station.id = :stationId
        AND m.startTime >= :from
        AND m.startTime < :to
    """)
    Double calculateAverageBetween(
            @Param("stationId") Integer stationId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    Optional<Measurement> findTopByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByOzoneAsc(
            LocalDateTime from,
            LocalDateTime to
    );

    Optional<Measurement> findTopByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByOzoneDesc(
            LocalDateTime from,
            LocalDateTime to
    );

    Optional<Measurement> findTopByStation_IdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByOzoneAsc(
            Integer stationId,
            LocalDateTime from,
            LocalDateTime to
    );

    Optional<Measurement> findTopByStation_IdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByOzoneDesc(
            Integer stationId,
            LocalDateTime from,
            LocalDateTime to
    );

    List<Measurement> findByOzoneGreaterThanEqualAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
            BigDecimal threshold,
            LocalDateTime from,
            LocalDateTime to
    );

    List<Measurement> findByStation_IdAndOzoneGreaterThanEqualAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
            Integer stationId,
            BigDecimal threshold,
            LocalDateTime from,
            LocalDateTime to
    );
}
