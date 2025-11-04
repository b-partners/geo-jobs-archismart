package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.community.CommunityUsedSurface;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunityUsedSurfaceRepository
    extends JpaRepository<CommunityUsedSurface, String> {
  List<CommunityUsedSurface> findByCommunityAuthorizationIdOrderByUsageDatetimeDesc(
      String communityId, Pageable pageable);
}
