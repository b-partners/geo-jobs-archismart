package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.community.RevokedApiKey;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RevokedApiKeyRepository extends JpaRepository<RevokedApiKey, String> {
  Optional<RevokedApiKey> findByRevokedApiKeyValue(String apiKeyValue);
}
