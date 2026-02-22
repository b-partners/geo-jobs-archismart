package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunityAuthorizationRepository
    extends JpaRepository<CommunityAuthorization, String> {

  @Query(
      value =
          """
          SELECT DISTINCT c.* FROM community_authorization c
          LEFT JOIN community_authorization_api_key k ON k.id_community_authorization_owner = c.id
          WHERE c.api_key = :key
             OR c.dashboard_api_key = :key
             OR k.key_value = :key
          LIMIT 1
          """,
      nativeQuery = true)
  Optional<CommunityAuthorization> findByApiKey(@Param("key") String apiKey);

  Optional<CommunityAuthorization> findByEmail(String email);

  @Query(
      """
      SELECT DISTINCT c FROM community_authorization c
      LEFT JOIN c.apiKeys k
      WHERE c.apiKey = :key OR c.dashboardApiKey = :key OR k.keyValue = :key
      """)
  Optional<CommunityAuthorization> findByDashboardApiKey(@Param("key") String actualKey);
}
