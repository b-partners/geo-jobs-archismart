package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface CityJSONRequestRepository extends JpaRepository<CityJSONRequest, String> {
  Optional<CityJSONRequest> findByIdAndCommunityOwnerId(String id, String communityOwnerId);

  @Transactional
  @Modifying
  @Query(
      "UPDATE CityJSONRequest r SET r.dashboardRegistrationDatetime = :datetime WHERE r.id = :id")
  void markDashboardRegistered(@Param("id") String id, @Param("datetime") Instant datetime);
}
