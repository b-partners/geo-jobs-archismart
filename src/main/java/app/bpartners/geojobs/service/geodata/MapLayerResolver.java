package app.bpartners.geojobs.service.geodata;

import static java.math.RoundingMode.HALF_UP;
import static java.time.Instant.now;

import app.bpartners.geojobs.repository.CachedAreaPictureMapLayerRepository;
import app.bpartners.geojobs.repository.model.CachedAreaPictureMapLayer;
import app.bpartners.geojobs.service.geodata.model.GeodataMapLayer;
import app.bpartners.geojobs.service.geodata.model.MapLayerActual;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MapLayerResolver {
  static final int CACHE_KEY_COORDINATES_SCALE = 3;
  static final Duration CACHE_ENTRY_LIFETIME = Duration.ofDays(30);

  private final GeodataMapLayerClient mapLayerClient;
  private final CachedAreaPictureMapLayerRepository cachedMapLayerRepository;

  public Optional<GeodataMapLayer> apply(double longitude, double latitude) {
    var cacheKey = cacheKeyOf(longitude, latitude);
    var cached = cachedMapLayerRepository.findById(cacheKey).filter(this::isStillFresh);
    if (cached.isPresent()) {
      return cached.map(MapLayerResolver::toMapLayer);
    }

    var retrieved =
        mapLayerClient.getActualMapLayer(latitude, longitude).map(MapLayerActual::layer);
    retrieved
        .filter(layer -> layer.name() != null && layer.precisionLevelInCm() != null)
        .ifPresent(layer -> cache(cacheKey, layer));
    return retrieved;
  }

  private void cache(String cacheKey, GeodataMapLayer layer) {
    cachedMapLayerRepository.save(
        CachedAreaPictureMapLayer.builder()
            .id(cacheKey)
            .layerName(layer.name())
            .precisionLevelInCm(layer.precisionLevelInCm())
            .layerYear(layer.year())
            .layerSource(layer.source())
            .cachedAt(now())
            .build());
  }

  private boolean isStillFresh(CachedAreaPictureMapLayer cached) {
    return cached.getCachedAt() != null
        && Duration.between(cached.getCachedAt(), now()).compareTo(CACHE_ENTRY_LIFETIME) < 0;
  }

  private static GeodataMapLayer toMapLayer(CachedAreaPictureMapLayer cached) {
    return new GeodataMapLayer(
        null,
        cached.getLayerName(),
        cached.getLayerYear(),
        cached.getLayerSource(),
        null,
        null,
        cached.getPrecisionLevelInCm());
  }

  static String cacheKeyOf(double longitude, double latitude) {
    return rounded(longitude) + "," + rounded(latitude);
  }

  private static String rounded(double coordinate) {
    return BigDecimal.valueOf(coordinate)
        .setScale(CACHE_KEY_COORDINATES_SCALE, HALF_UP)
        .toPlainString();
  }
}
