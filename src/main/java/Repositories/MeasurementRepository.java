package Repositories;

import at.kaindorf.climate_project.pojo.Measurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MeasurementRepository extends JpaRepository<Measurement, Long> {
    Optional<Measurement> findByStartTime(LocalDateTime startTime);

    boolean existsByStartTime(LocalDateTime startTime);
}
