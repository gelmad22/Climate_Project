package at.kaindorf.climate_project.repositories;

import at.kaindorf.climate_project.pojo.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StationRepository extends JpaRepository<Station, Integer> {
}
