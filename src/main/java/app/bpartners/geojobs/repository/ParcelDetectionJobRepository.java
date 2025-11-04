package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.detection.ParcelDetectionJob;
import org.springframework.stereotype.Repository;

@Repository
public interface ParcelDetectionJobRepository extends JobRepository<ParcelDetectionJob> {}
