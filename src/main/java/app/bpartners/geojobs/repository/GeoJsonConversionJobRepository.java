package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionJob;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface GeoJsonConversionJobRepository extends JobRepository<GeoJsonConversionJob> {
  List<GeoJsonConversionJob> findByZoneDetectionJobId(String jobId);
}
