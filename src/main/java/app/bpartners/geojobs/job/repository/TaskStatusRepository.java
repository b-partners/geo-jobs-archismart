package app.bpartners.geojobs.job.repository;

import app.bpartners.geojobs.job.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskStatusRepository extends JpaRepository<TaskStatus, String> {}
