package app.bpartners.geojobs.service.roofer3dbag.conf;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Propriétés de configuration du client 3DBag/roofer.
 *
 * <p>Les valeurs peuvent être définies via : - variables d'environnement : ROOFER_API_KEY,
 * ROOFER_API_BASE_URL - ou propriétés Spring : roofer.api-key, roofer.base-url
 */
@Getter
@Component
public class RooferApiProperties {
  private static final int CONNECT_TIMEOUT_MS = 10_000;
  private static final int READ_TIMEOUT_MS = 300_000;

  /** URL de base de l'API. Valeur par défaut tirée de la spec OAS3. */
  private final String baseUrl;

  /**
   * Clé d'API envoyée dans l'en-tête x-api-key. À fournir via la variable d'environnement
   * ROOFER_API_KEY.
   */
  private final String apiKey;

  /** Timeout de connexion en millisecondes. */
  private final int connectTimeoutMs;

  /** Timeout de lecture en millisecondes (la génération peut être longue). */
  private final int readTimeoutMs;

  public RooferApiProperties(
      @Value("${roofer.3d.bag.base.url}") String baseUrl,
      @Value("${roofer.3d.bag.api.key}") String apiKey) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.connectTimeoutMs = CONNECT_TIMEOUT_MS;
    this.readTimeoutMs = READ_TIMEOUT_MS;
  }
}
