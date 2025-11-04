package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MachineDetectedTileRepository extends JpaRepository<MachineDetectedTile, String> {

  // TODO: use JPQL instead
  @Query(
      value =
          "select distinct d.* from detected_tile d"
              + " join detected_object dobj on dobj.detected_tile_id=d.id"
              + " join detectable_object_type dot on dobj.id = dot.object_id"
              + " where d.zdj_job_id = :zoneDetectionJobId"
              + " and dot.detectable_type = cast(:detectableType as detectable_type)",
      nativeQuery = true)
  List<MachineDetectedTile> findAllByZdjJobIdAndDetectableType(
      @Param("zoneDetectionJobId") String zoneDetectionJobId,
      @Param("detectableType") String detectableType,
      Pageable pageable);

  // TODO: use JPQL instead
  @Query(
      value =
          "select count(distinct d.id) from detected_tile d"
              + " join detected_object dobj on dobj.detected_tile_id=d.id"
              + " join detectable_object_type dot on dobj.id = dot.object_id"
              + " where d.zdj_job_id = :zoneDetectionJobId"
              + " and dot.detectable_type = cast(:detectableType as detectable_type)",
      nativeQuery = true)
  Long countByZdjJobIdAndDetectableType(
      @Param("zoneDetectionJobId") String zoneDetectionJobId,
      @Param("detectableType") String detectableType);

  Long countByZdjJobId(String jobId);

  List<MachineDetectedTile> findAllByZdjJobId(String id, Pageable pageable);

  List<MachineDetectedTile> findAllByZdjJobId(String id);

  List<MachineDetectedTile> findAllByParcelId(String parcelId);

  @Query(
      value = "select * from get_tiles_without_detected_object(:zoneDetectionJobId)",
      nativeQuery = true)
  List<MachineDetectedTile> findAllInDoubtTilesWithoutObjectByZdjJobId(
      @Param("zoneDetectionJobId") String zoneDetectionJobId);

  @Query(
      value =
          "select * from get_in_doubt_detected_tiles(:zoneDetectionJobId,:minConfidenceForDelivery,"
              + " :isGreater)",
      nativeQuery = true)
  List<MachineDetectedTile> findAllInDoubtByZdjJobId(
      @Param("zoneDetectionJobId") String zoneDetectionJobId,
      @Param("minConfidenceForDelivery") double minConfidenceForDelivery,
      @Param("isGreater") Boolean isGreater);

  // TODO : use JPQL and retrieve in doubt detected tiles without filter
  @Query(
      value =
          "select case when true_positive_count = 0 then false_positive_count else"
              + " true_positive_count end from (select count(id) as true_positive_count from"
              + " get_in_doubt_detected_tiles(:zoneDetectionJobId, :minConfidenceForDelivery,"
              + " true)) as true_positive,(select count(id) as false_positive_count from"
              + " get_in_doubt_detected_tiles(:zoneDetectionJobId, :minConfidenceForDelivery,"
              + " false)) as false_positive",
      nativeQuery = true)
  Long countInDoubtDetectedTileToDeliveryByZdjJobId(
      @Param("zoneDetectionJobId") String zoneDetectionJobId,
      @Param("minConfidenceForDelivery") double minConfidenceForDelivery);
}
