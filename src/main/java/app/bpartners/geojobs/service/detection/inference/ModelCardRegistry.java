package app.bpartners.geojobs.service.detection.inference;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ModelCardRegistry {
  private final InferenceApiClient inferenceApiClient;
  private final Duration ttl;
  private final Clock clock;
  private volatile Snapshot snapshot;

  @Autowired
  public ModelCardRegistry(
      InferenceApiClient inferenceApiClient,
      @Value("${inference.api.model-cards.ttl:PT8H}") Duration ttl) {
    this(inferenceApiClient, ttl, Clock.systemUTC());
  }

  ModelCardRegistry(InferenceApiClient inferenceApiClient, Duration ttl, Clock clock) {
    this.inferenceApiClient = inferenceApiClient;
    this.ttl = ttl;
    this.clock = clock;
  }

  public Optional<DeployedModelCard> findById(String modelCardId) {
    return Optional.ofNullable(current().modelCardsById().get(modelCardId));
  }

  public Map<String, DeployedModelCard> getRunnableModelCards() {
    return current().modelCardsById();
  }

  public synchronized void invalidate() {
    snapshot = null;
  }

  private Snapshot current() {
    var local = snapshot;
    if (local != null && local.isFresh(clock.instant())) {
      return local;
    }
    return refresh();
  }

  private synchronized Snapshot refresh() {
    var local = snapshot;
    var now = clock.instant();
    if (local != null && local.isFresh(now)) {
      return local;
    }
    try {
      var modelCardsById =
          inferenceApiClient.getModelCards().stream()
              .filter(DeployedModelCard::isRunnable)
              .collect(toUnmodifiableMap(DeployedModelCard::modelCardId, identity(), (a, b) -> b));
      snapshot = new Snapshot(modelCardsById, now.plus(ttl));
      log.info("Loaded {} runnable model cards from inference API", modelCardsById.size());
      return snapshot;
    } catch (RuntimeException e) {
      if (local == null) {
        throw e;
      }
      log.warn(
          "Unable to refresh model cards, serving {} stale ones: {}",
          local.modelCardsById().size(),
          e.getMessage());
      return local;
    }
  }

  private record Snapshot(Map<String, DeployedModelCard> modelCardsById, Instant expiresAt) {
    boolean isFresh(Instant now) {
      return now.isBefore(expiresAt);
    }
  }
}
