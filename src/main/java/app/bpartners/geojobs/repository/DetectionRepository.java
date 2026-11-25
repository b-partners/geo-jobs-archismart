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

  @Query(
      """
    SELECT d.id FROM Detection d
    WHERE d.communityOwnerId = :communityOwnerId
      AND d.zoneHash = :zoneHash
      AND d.inferenceSignature = :inferenceSignature
      AND d.integrationTest = :integrationTest
      AND d.geojsonS3FileKey IS NOT NULL
      AND d.id <> :excludedId
    ORDER BY d.creationDatetime DESC
""")
  List<String> findReusableSourceIds(
      @Param("communityOwnerId") String communityOwnerId,
      @Param("zoneHash") String zoneHash,
      @Param("inferenceSignature") String inferenceSignature,
      @Param("integrationTest") boolean integrationTest,
      @Param("excludedId") String excludedId,
      Pageable pageable);

  @Query(
      """
    SELECT d.id FROM Detection d
    WHERE d.communityOwnerId = :communityOwnerId
      AND d.inferenceSignature = :inferenceSignature
      AND d.integrationTest = :integrationTest
      AND d.geojsonS3FileKey IS NOT NULL
      AND d.id <> :excludedId
      AND d.zoneMinLon BETWEEN :minLon - :toleranceLon AND :minLon + :toleranceLon
      AND d.zoneMinLat BETWEEN :minLat - :toleranceLat AND :minLat + :toleranceLat
      AND d.zoneMaxLon BETWEEN :maxLon - :toleranceLon AND :maxLon + :toleranceLon
      AND d.zoneMaxLat BETWEEN :maxLat - :toleranceLat AND :maxLat + :toleranceLat
    ORDER BY d.creationDatetime DESC
""")
  List<String> findReusableSourceIdsNear(
      @Param("communityOwnerId") String communityOwnerId,
      @Param("inferenceSignature") String inferenceSignature,
      @Param("integrationTest") boolean integrationTest,
      @Param("excludedId") String excludedId,
      @Param("minLon") double minLon,
      @Param("minLat") double minLat,
      @Param("maxLon") double maxLon,
      @Param("maxLat") double maxLat,
      @Param("toleranceLon") double toleranceLon,
      @Param("toleranceLat") double toleranceLat,
      Pageable pageable);
}
