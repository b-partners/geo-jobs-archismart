package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.repository.model.DetectionAddressConversionTask;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DetectionAddressConversionTaskRepository
    extends TaskRepository<DetectionAddressConversionTask> {
  @Query(
      value =
          "select dact.*"
              + "from detection_address_conversion_task dact,"
              + "     (select ts.task_id, progression, health"
              + "      from task_status ts,"
              + "           (select task_id, max(creation_datetime) as latest_creation_datetime"
              + "            from task_status"
              + "            group by task_id) actual_ts"
              + "      where ts.task_id = actual_ts.task_id"
              + "        and ts.creation_datetime = latest_creation_datetime) actual_task_status"
              + " where dact.id = actual_task_status.task_id"
              + "  and dact.job_id = :jobId"
              + "  and progression = cast(:progressionStatus as progression_status)"
              + "  and health = cast(:healthStatus as health_status)",
      nativeQuery = true)
  List<DetectionAddressConversionTask> findAllByJobIdAndProgressionStatusAndHealthStatus(
      @Param("jobId") String jobId,
      @Param("progressionStatus") String progressionStatus,
      @Param("healthStatus") String healthStatus);
}
