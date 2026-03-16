package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.geocoding.GeoCodingJob;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeoCodingJobRepository extends JpaRepository<GeoCodingJob, String> {
  Optional<GeoCodingJob> findByEndToEndIdAndCommunityOwnerId(
      String endToEndId, String communityOwnerId);
}
