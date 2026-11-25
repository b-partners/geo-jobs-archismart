package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.detection.ModelCardVersion;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ModelCardVersionRepository extends JpaRepository<ModelCardVersion, String> {
  @Query(
      """
    SELECT m FROM ModelCardVersion m
    WHERE m.modelCardId IN :modelCardIds
      AND m.version = (SELECT MAX(v.version) FROM ModelCardVersion v
                       WHERE v.modelCardId = m.modelCardId)
""")
  List<ModelCardVersion> findLatestByModelCardIdIn(
      @Param("modelCardIds") Collection<String> modelCardIds);
}
