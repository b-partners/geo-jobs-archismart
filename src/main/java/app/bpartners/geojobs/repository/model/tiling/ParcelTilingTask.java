package app.bpartners.geojobs.repository.model.tiling;

import static app.bpartners.geojobs.repository.model.GeoJobType.TILING;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.repository.model.GeoJobType;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Entity(name = "parcel_tiling_task")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@Getter
@Setter
@JsonIgnoreProperties({"status"})
@ToString
public class ParcelTilingTask extends Task implements Serializable {
  @ManyToMany(cascade = ALL, fetch = EAGER)
  @JoinTable(
      name = "parcel_with_tiling_task",
      joinColumns = @JoinColumn(name = "id_tiling_task"),
      inverseJoinColumns = @JoinColumn(name = "id_parcel"))
  private List<Parcel> parcels;

  @Override
  public GeoJobType getJobType() {
    return TILING;
  }

  @Override
  public Task semanticClone() {
    return this.toBuilder().statusHistory(new ArrayList<>(getStatusHistory())).build();
  }

  public ParcelContent getParcelContent() {
    return getParcel() == null ? null : getParcel().getParcelContent();
  }

  public String getParcelId() {
    return parcels.isEmpty() ? null : getParcel().getId();
  }

  public String getParcelContentId() {
    return parcels.isEmpty() ? null : getParcelContent().getId();
  }

  public Parcel getParcel() {
    if (parcels == null || parcels.isEmpty()) return null;
    var chosenParcel = parcels.getFirst();
    if (parcels.size() > 1) {
      log.error(
          "[DEBUG] TilingTask(id={}) contains multiple parcels (size= {}) but only one"
              + " Parcel(id={}) is handle for now",
          getId(),
          parcels.size(),
          chosenParcel.getId());
    }
    return chosenParcel;
  }

  public List<Tile> getTiles() {
    return getParcel() == null ? null : getParcel().getParcelContent().getTiles();
  }

  public ParcelTilingTask duplicate(
      String taskId,
      String jobId,
      String parcelId,
      String parcelContentId,
      boolean hasSameStatuses,
      boolean hasSameTile) {
    return ParcelTilingTask.builder()
        .id(taskId)
        .jobId(jobId)
        .parcels(
            parcels == null
                ? null
                : parcels.stream()
                    .map(parcel -> parcel.duplicate(parcelId, parcelContentId, hasSameTile))
                    .toList())
        .statusHistory(
            List.of(
                this.getStatus()
                    .duplicate(
                        hasSameStatuses ? this.getStatus().getId() : randomUUID().toString(),
                        taskId)))
        .submissionInstant(this.getSubmissionInstant())
        .build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ParcelTilingTask that = (ParcelTilingTask) o;
    return Objects.equals(parcels, that.parcels)
        && Objects.equals(getJobId(), that.getJobId())
        && Objects.equals(getStatusHistory(), that.getStatusHistory());
  }

  @Override
  public int hashCode() {
    return Objects.hash(parcels, getJobId(), getStatusHistory());
  }

  public String describe() {
    return "TilingTask(id="
        + this.getId()
        + ", tiles.size="
        + getParcelContent().getTiles().size()
        + ", status="
        + getStatus()
        + ")";
  }
}
