package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskStatisticRepository extends JpaRepository<TaskStatistic, String> {
  TaskStatistic findTopByJobIdOrderByUpdatedAtDesc(String jobId);
}
