package at.kaindorf.climate_project.pojo;

import at.kaindorf.climate_project.annotations.ToDto;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Measurement {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ToDto
    private int ozone;

    @ToDto
    private LocalDate startTime;

    @ToDto
    private LocalDate endTime;


}
