package app.bpartners.geojobs.repository.model.detection;

import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static jakarta.persistence.FetchType.EAGER;

import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.GeoJobType;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Entity(name = "parcel_detection_task")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@Getter
@Setter
@ToString
@JsonIgnoreProperties({"status"})
public class ParcelDetectionTask extends Task implements Serializable {
  @ManyToMany(fetch = EAGER)
  @JoinTable(
      name = "parcel_with_detection_task",
      joinColumns = @JoinColumn(name = "id_detection_task"),
      inverseJoinColumns = @JoinColumn(name = "id_parcel"))
  private List<Parcel> parcels;

  public List<Tile> getTiles() {
    return getParcel() == null ? null : getParcel().getParcelContent().getTiles();
  }

  public Parcel getParcel() {
    if (parcels == null || parcels.isEmpty()) return null;
    var chosenParcel = parcels.getFirst();
    if (parcels.size() > 1) {
      log.error(
          "DetectionTask(id={}) contains multiple parcels but only one Parcel(id={}) is handle for"
              + " now",
          getId(),
          chosenParcel.getId());
    }
    return chosenParcel;
  }

  public String getAddress() {
    if (getParcel() == null
        || getParcel().getParcelContent() == null
        || getParcel().getParcelContent().getFeature() == null
        || getParcel().getParcelContent().getFeature().getProperties() == null
        || getParcel().getParcelContent().getFeature().getProperties().get("address") == null)
      return null;
    return (String) getParcel().getParcelContent().getFeature().getProperties().get("address");
  }

  @SneakyThrows
  public Feature getPoint() {
    if (getParcel() == null
        || getParcel().getParcelContent() == null
        || getParcel().getParcelContent().getFeature() == null
        || getParcel().getParcelContent().getFeature().getProperties() == null
        || getParcel().getParcelContent().getFeature().getProperties().get("point") == null) {
      return null;
    }
    log.info(
        "Feature point retrieved {}",
        getParcel().getParcelContent().getFeature().getProperties().get("point"));
    return new ObjectMapper()
        .findAndRegisterModules()
        .readValue(
            getParcel().getParcelContent().getFeature().getProperties().get("point").toString(),
            Feature.class);
  }

  @Override
  public GeoJobType getJobType() {
    return DETECTION;
  }

  @Override
  public ParcelDetectionTask semanticClone() {
    return this.toBuilder().statusHistory(new ArrayList<>(getStatusHistory())).build();
  }
}
