package app.bpartners.geojobs.job.repository;

import app.bpartners.geojobs.job.model.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobStatusRepository extends JpaRepository<JobStatus, String> {}
