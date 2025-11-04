package app.bpartners.geojobs.job.model;

import app.bpartners.geojobs.job.repository.JobTypeConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@PrimaryKeyJoinColumn(name = "id")
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Table(name = "job_status")
public class JobStatus extends Status {
  @Getter
  @Setter
  @JoinColumn(referencedColumnName = "id")
  private String jobId;

  @Convert(converter = JobTypeConverter.class)
  private JobType jobType;

  public JobType getJobType() {
    return jobType;
  }

  // TODO(status.jobType-serialization): have to disable deserialization as JobType is abstract
  //  Fix is given at https://www.baeldung.com/jackson-inheritance
  //  Note that this mess is due to the fact that we have one model for domain, db and event!
  @JsonIgnore
  public void setJobType(JobType jobType) {
    this.jobType = jobType;
  }

  public static JobStatus from(String id, Status status, JobType jobType) {
    return JobStatus.builder()
        .jobId(id)
        .id(status.getId())
        .jobType(jobType)
        .progression(status.getProgression())
        .health(status.getHealth())
        .message(status.getMessage())
        .creationDatetime(status.getCreationDatetime())
        .build();
  }
}
