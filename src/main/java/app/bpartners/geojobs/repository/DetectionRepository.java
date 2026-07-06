package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.detection.Detection;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DetectionRepository extends JpaRepository<Detection, String> {
  Optional<Detection> findByEndToEndId(String endToEndId);

  Optional<Detection> findByZtjId(String ztjId);

  Optional<Detection> findByZdjId(String ztjId);

  List<Detection> findByCommunityOwnerIdAndCreationDatetimeBetweenOrderByCreationDatetimeDesc(
      String communityOwnerId, Instant from, Instant to, Pageable pageable);

  @Query(
      """
    SELECT d FROM Detection d
    WHERE d.communityOwnerId = :communityOwnerId
      AND d.creationDatetime BETWEEN :from AND :to
      AND (d.geojsonS3FileKey IS NOT NULL or d.vggFileKey IS NOT NULL)
    ORDER BY d.creationDatetime DESC
""")
  List<Detection> findByCommunityOwnerIdAndCriteria(
      @Param("communityOwnerId") String communityOwnerId,
      @Param("from") Instant from,
      @Param("to") Instant to,
      Pageable pageable);

  List<Detection>
      findByCommunityOwnerIdAndCreationDatetimeBetweenAndZoneNameIsContainingIgnoreCaseOrderByCreationDatetimeDesc(
          String communityOwnerId, Instant from, Instant to, String zoneName, Pageable pageable);

  List<Detection> findAllByCreationDatetimeBetweenOrderByCreationDatetimeDesc(
      Instant from, Instant to, Pageable pageable);

  List<Detection>
      findAllByCreationDatetimeBetweenAndZoneNameIsContainingIgnoreCaseOrderByCreationDatetimeDesc(
          Instant from, Instant to, String zoneName, Pageable pageable);

  Optional<Detection> findByEndToEndIdAndCommunityOwnerId(
      String endToEndId, String communityOwnerId);
}
