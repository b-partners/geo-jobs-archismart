package app.bpartners.geojobs.service.area.mutation;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.DetectionMaskFromTileRetriever;
import app.bpartners.geojobs.service.area.mutation.model.AreaPictureHistoryResponse;
import app.bpartners.geojobs.service.area.mutation.model.MutationContext;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Component;

/** Builds the {@link MutationContext} used to detect a roof mutation for a detection. */
@Component
@RequiredArgsConstructor
public class MutationContextFactory {
  private final DetectionMaskFromTileRetriever maskFromTileRetriever;
  private final MachineDetectedTileRepository machineDetectedTileRepository;
  private final GeometryConverter geometryConverter;
  private final GeodataApi geodataApi;

  public MutationContext create(Detection detection, Geometry roofGeometry) {
    var roofMultiPolygon = asMultiPolygon(roofGeometry);
    var tile = findTileIntersecting(detection, roofMultiPolygon);
    var maskImageFile = maskFromTileRetriever.apply(tile, roofMultiPolygon);

    // The mask above is rasterized against `tile`'s x/y/z (z=20, the same zoom as
    // bpartners-geodata's HOUSES_0), so a point inside that same tile keeps the area picture
    // history images pixel-aligned with the mask.
    var centroid = roofGeometry.getCentroid();
    var history = geodataApi.getAreaPictureHistory(centroid.getX(), centroid.getY());
    var images = history.images();
    if (images == null || images.size() < 2) {
      throw new IllegalStateException(
          "Expected 2 dated area pictures from the geodata API for detection " + detection.getId());
    }
    var sortedByYear =
        images.stream()
            .sorted(Comparator.comparingInt(AreaPictureHistoryResponse.DatedImage::year))
            .toList();
    var older = sortedByYear.getFirst();
    var mostRecent = sortedByYear.getLast();

    return new MutationContext(older, mostRecent, maskImageFile);
  }

  private Tile findTileIntersecting(Detection detection, MultiPolygon roofMultiPolygon) {
    return machineDetectedTileRepository.findAllByZdjJobId(detection.getZdjId()).stream()
        .map(MachineDetectedTile::getTile)
        .filter(tile -> tileMultiPolygon(tile).intersects(roofMultiPolygon))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "No tile intersects the roof to compute the mutation for detection "
                        + detection.getId()));
  }

  private MultiPolygon tileMultiPolygon(Tile tile) {
    var tileCoordinates = tile.getCoordinates();
    return geometryConverter.getMultiPolygonFromTile(
        tileCoordinates.getX(), tileCoordinates.getY(), tileCoordinates.getZ());
  }

  private static MultiPolygon asMultiPolygon(Geometry geometry) {
    if (geometry instanceof MultiPolygon multiPolygon) {
      return multiPolygon;
    }
    if (geometry instanceof Polygon polygon) {
      return geometryFactory.createMultiPolygon(new Polygon[] {polygon});
    }
    throw new IllegalArgumentException(
        "Unsupported geometry type to build the mutation mask: " + geometry.getClass());
  }
}
