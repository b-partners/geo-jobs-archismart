package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.community.CommunityAuthorizationApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityAuthorizationApiKeyRepository
    extends JpaRepository<CommunityAuthorizationApiKey, String> {}
