package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunityAuthorizationRepository
    extends JpaRepository<CommunityAuthorization, String> {
  Optional<CommunityAuthorization> findByApiKey(String apiKey);

  Optional<CommunityAuthorization> findByEmail(String email);
}
