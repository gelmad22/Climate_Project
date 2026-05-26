package at.kaindorf.climate_project.repositories;

import at.kaindorf.climate_project.pojo.Measurement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeasurementRepository extends JpaRepository<Measurement, Long> {

    Optional<Measurement> findFirstByStartTimeOrderByIdAsc(LocalDateTime startTime);

    boolean existsByStartTime(LocalDateTime startTime);

    boolean existsByStation_IdAndStartTime(Integer stationId, LocalDateTime startTime);

    List<Measurement> findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
            LocalDateTime from,
            LocalDateTime to
    );

    long countByStartTimeGreaterThanEqualAndStartTimeLessThan(
            LocalDateTime from,
            LocalDateTime to
    );

    @Query("""
        SELECT AVG(m.ozone)
        FROM Measurement m
        WHERE m.startTime >= :from
        AND m.startTime < :to
    """)
    Double calculateAverageBetween(LocalDateTime from, LocalDateTime to);

    @Query("""
        SELECT MIN(m.ozone)
        FROM Measurement m
        WHERE m.startTime >= :from
        AND m.startTime < :to
    """)
    BigDecimal findMinOzoneBetween(LocalDateTime from, LocalDateTime to);

    @Query("""
        SELECT MAX(m.ozone)
        FROM Measurement m
        WHERE m.startTime >= :from
        AND m.startTime < :to
    """)
    BigDecimal findMaxOzoneBetween(LocalDateTime from, LocalDateTime to);

    Optional<Measurement> findTopByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByOzoneAsc(
            LocalDateTime from,
            LocalDateTime to
    );

    Optional<Measurement> findTopByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByOzoneDesc(
            LocalDateTime from,
            LocalDateTime to
    );

    List<Measurement> findTop10ByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByOzoneDesc(
            LocalDateTime from,
            LocalDateTime to
    );

    List<Measurement> findByOzoneGreaterThanEqualAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
            BigDecimal threshold,
            LocalDateTime from,
            LocalDateTime to
    );

    @Query("""
        SELECT m
        FROM Measurement m
        WHERE m.startTime >= :from
        AND m.startTime < :to
        ORDER BY m.ozone DESC
    """)
    List<Measurement> findTopValuesBetween(
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );
}
