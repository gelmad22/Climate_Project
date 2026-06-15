package at.kaindorf.climate_project.pojo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stations")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Station {
    @Id
    private Integer id;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer id;

        private Builder() {
        }

        public Builder id(Integer id) {
            this.id = id;
            return this;
        }

        public Station build() {
            if (id == null) {
                throw new IllegalStateException("Station id must not be null");
            }

            return new Station(id);
        }
    }
}
