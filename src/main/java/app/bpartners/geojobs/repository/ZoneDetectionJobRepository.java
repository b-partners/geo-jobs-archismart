package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface ZoneDetectionJobRepository extends JobRepository<ZoneDetectionJob> {
  List<ZoneDetectionJob> findAllByZoneTilingJob_Id(String tilingJobId);

  ZoneDetectionJob findByZoneTilingJobId(String tilingJobId);
}
