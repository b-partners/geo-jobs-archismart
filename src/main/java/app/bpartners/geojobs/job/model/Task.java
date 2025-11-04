package app.bpartners.geojobs.job.model;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Getter
@Setter
@ToString
@MappedSuperclass
@JsonIgnoreProperties({"status"})
public abstract class Task implements Serializable, Statusable<TaskStatus> {
  @Id private String id;

  private String jobId;
  @Nullable String asJobId;
  @Getter @CreationTimestamp private Instant submissionInstant;

  @OneToMany(cascade = ALL, mappedBy = "taskId", fetch = EAGER)
  @Builder.Default
  private List<TaskStatus> statusHistory = new ArrayList<>();

  public abstract JobType getJobType();

  public boolean isPending() {
    return PENDING.equals(getStatus().getProgression());
  }

  public boolean isFinished() {
    return FINISHED.equals(getStatus().getProgression());
  }

  public boolean isSucceeded() {
    return FINISHED.equals(getStatus().getProgression())
        && SUCCEEDED.equals(getStatus().getHealth());
  }

  public boolean isFailed() {
    return FINISHED.equals(getStatus().getProgression()) && FAILED.equals(getStatus().getHealth());
  }

  public abstract Task semanticClone();

  @Override
  public TaskStatus from(Status status) {
    return TaskStatus.from(id, status, getJobType());
  }

  @Override
  public void setStatusHistory(List<TaskStatus> statusHistory) {
    if (statusHistory == null) {
      this.statusHistory = new ArrayList<>();
      return;
    }
    this.statusHistory = statusHistory;
  }
}
