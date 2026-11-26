package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface CityJSONRequestRepository extends JpaRepository<CityJSONRequest, String> {
  Optional<CityJSONRequest> findByIdAndCommunityOwnerId(String id, String communityOwnerId);

  @Query(
      """
    SELECT r FROM CityJSONRequest r
    WHERE r.communityOwnerId = :communityOwnerId
      AND r.creationDatetime BETWEEN :from AND :to
      AND r.status = app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus.FINISHED
    ORDER BY r.creationDatetime DESC
""")
  List<CityJSONRequest> findSucceededByCommunityOwnerIdAndCriteria(
      @Param("communityOwnerId") String communityOwnerId,
      @Param("from") Instant from,
      @Param("to") Instant to,
      Pageable pageable);

  @Query(
      """
    SELECT r FROM CityJSONRequest r
    WHERE r.communityOwnerId = :communityOwnerId
      AND r.creationDatetime BETWEEN :from AND :to
    ORDER BY r.creationDatetime DESC
""")
  List<CityJSONRequest> findByCommunityOwnerIdAndCriteria(
      @Param("communityOwnerId") String communityOwnerId,
      @Param("from") Instant from,
      @Param("to") Instant to,
      Pageable pageable);

  @Transactional
  @Modifying
  @Query(
      "UPDATE CityJSONRequest r SET r.dashboardRegistrationDatetime = :datetime WHERE r.id = :id")
  void markDashboardRegistered(@Param("id") String id, @Param("datetime") Instant datetime);
}
