package app.bpartners.geojobs.service.geojson;

import static app.bpartners.geojobs.service.geojson.GeoJson.fromFeatures;

import app.bpartners.geojobs.model.DetectedTile;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GeoJsonConverter implements Converter<List<DetectedTile>, GeoJson> {
  private static final int DEFAULT_IMAGE_SIZE = 1024;
  private final GeoJsonMapper mapper;

  @Override
  public GeoJson convert(List<DetectedTile> detectedTiles) {
    List<GeoJson.GeoFeature> geoFeatures =
        detectedTiles.stream()
            .map(
                detectedTile -> {
                  var tile = detectedTile.getTile();
                  var xTile = tile.getCoordinates().getX();
                  var yTile = tile.getCoordinates().getY();
                  var zoom = tile.getCoordinates().getZ();
                  return mapper.toGeoFeatures(
                      xTile, yTile, zoom, DEFAULT_IMAGE_SIZE, detectedTile.getDetectedObjects());
                })
            .flatMap(List::stream)
            .toList();
    return fromFeatures(geoFeatures);
  }
}
