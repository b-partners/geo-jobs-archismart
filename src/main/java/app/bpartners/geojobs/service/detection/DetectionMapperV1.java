package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.model.CustomObjectMapper.objectMapper;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static app.bpartners.geojobs.service.detection.DetectionMapper.*;
import static app.bpartners.geojobs.service.detection.DetectionResponse.REGION_CONFIDENCE_PROPERTY;
import static app.bpartners.geojobs.service.detection.DetectionResponse.REGION_LABEL_PROPERTY;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.tiling.TileValidator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Legacy (V1) counterpart of {@link DetectionMapper}: maps a {@link DetectionResponse} (the {@code
 * Rst_raw} shape) into {@link DetectedObject}s. Kept alongside the V2 mapper so callers still
 * holding a V1 response can extract detected objects without converting first.
 */
@Slf4j
@Component
@AllArgsConstructor
public class DetectionMapperV1 {
  private final TileValidator tileValidator;

  public MachineDetectedTile toDetectedTile(
      DetectionResponse detectionResponse,
      Tile tile,
      String parcelId,
      String zdjJobId,
      String parcelJobId) {
    String detectedTileId = randomUUID().toString();
    var tileCoordinates = tile.getCoordinates();
    tileValidator.accept(tile);

    var fileDataList = detectionResponse.getRstRaw().values().stream().toList();

    List<DetectedObject> machineDetectedObjects = new ArrayList<>();
    fileDataList.forEach(
        fileData -> {
          List<DetectionResponse.ImageData.Region> regions =
              fileData.getRegions().values().stream().toList();
          machineDetectedObjects.addAll(
              regions.stream()
                  .map(region -> toDetectedObject(region, detectedTileId, tileCoordinates.getZ()))
                  .toList());
        });

    return MachineDetectedTile.builder()
        .id(detectedTileId)
        .zdjJobId(zdjJobId)
        .parcelJobId(parcelJobId)
        .parcelId(parcelId)
        .tile(tile)
        .bucketPath(tile.getBucketPath())
        .detectedObjects(machineDetectedObjects)
        .creationDatetime(now())
        .build();
  }

  public DetectedObject toDetectedObject(
      DetectionResponse.ImageData.Region region, String detectedTileId, Integer zoom) {
    var regionAttributes = region.getRegionAttributes();
    var label = regionAttributes.get(REGION_LABEL_PROPERTY);
    Double confidence = null;
    try {
      if (regionAttributes.containsKey(REGION_CONFIDENCE_PROPERTY)) {
        confidence = Double.valueOf(regionAttributes.get(REGION_CONFIDENCE_PROPERTY));
      }

    } catch (NumberFormatException ignored) {
    }
    var polygon = region.getShapeAttributes();
    var objectId = randomUUID().toString();
    return DetectedObject.builder()
        .id(objectId)
        .detectedTileId(detectedTileId)
        .type(MACHINE)
        .detectedObjectType(
            DetectableObjectType.builder()
                .id(randomUUID().toString())
                .objectId(objectId)
                .detectableType(toDetectableType(label))
                .build())
        .feature(toFeature(polygon, zoom))
        .computedConfidence(confidence)
        .build();
  }

  private DetectableType toDetectableType(String label) {
    return switch (label.toUpperCase()) {
      case ROOF_STRING_VALUE, TOITURE_REVETEMENT_STRING_VALUE -> DetectableType.TOITURE_REVETEMENT;
      case SOLAR_PANEL_STRING_VALUE, PV_STRING_VALUE, PANNEAU_PHOTOVOLTAIQUE_STRING_VALUE ->
          DetectableType.PANNEAU_PHOTOVOLTAIQUE;
      case TREE_STRING_VALUE, ARBRE_STRING_VALUE -> DetectableType.ARBRE;
      case PATHWAY_STRING_VALUE, PASSAGE_PIETON_STRING_VALUE -> DetectableType.PASSAGE_PIETON;
      case POOL_STRING_VALUE, PISCINE_STRING_VALUE -> DetectableType.PISCINE;
      case BATI_STRING_VALUE -> DetectableType.BATI;
      case BATI_TUILES_STRING_VALUE, ROOF_TUILES_STRING_VALUE -> DetectableType.BATI_TUILES;
      case BATI_BETON_STRING_VALUE -> DetectableType.BATI_BETON;
      case BATI_ARDOISE_STRING_VALUE, ROOF_ARDOISE_STRING_VALUE -> DetectableType.BATI_ARDOISE;
      case BATI_AUTRES_STRING_VALUE, ROOF_AUTRES_STRING_VALUE -> DetectableType.BATI_AUTRES;
      case LINE_STRING_VALUE -> DetectableType.LINE;
      case TROTTOIR_STRING_VALUE, SIDEWALK_STRING_VALUE -> DetectableType.TROTTOIR;
      case PARKING_STRING_VALUE -> DetectableType.PARKING;
      case ESPACE_VERT_STRING_VALUE, GREEN_SPACE_STRING_VALUE -> DetectableType.ESPACE_VERT;
      case OBSTACLE_STRING_VALUE -> DetectableType.OBSTACLE;
      case CHEMINEE_STRING_VALUE -> DetectableType.CHEMINEE;
      case VELUX_STRING_VALUE -> DetectableType.VELUX;
      case USURE_IMPORTANTE_ARDOISE_STRING_VALUE, USURE_IMPORTANTE_TUILES_STRING_VALUE ->
          DetectableType.USURE_IMPORTANTE;
      case USURE_LEGERE_ARDOISE_STRING_VALUE, USURE_LEGERE_AUTRES_STRING_VALUE ->
          DetectableType.USURE_LEGER;
      case MOISISSURE_COULEUR_ARDOISE_STRING_VALUE, MOISISSURE_COULEUR_TUILES_STRING_VALUE ->
          DetectableType.MOISISSURE_COULEUR;
      case MOISISSURE_NOIRCIE_TUILES_STRING_VALUE -> DetectableType.MOISISSURE_NOIRCIE;
      case MOISISSURE_CLAIR_TUILES_STRING_VALUE -> DetectableType.MOISISSURE_CLAIR;
      case HUMIDITE_CLAIR_AUTRES_STRING_VALUE -> DetectableType.HUMIDITE_CLAIR;
      case HUMIDITE_INTENSE_AUTRES_STRING_VALUE -> DetectableType.HUMIDITE_INTENSE;
      default -> throw new IllegalStateException("Unexpected value: " + label.toLowerCase());
    };
  }

  @SneakyThrows
  private app.bpartners.geojobs.repository.model.Feature toFeature(
      DetectionResponse.ImageData.ShapeAttributes shapeAttributes, int zoom) {
    List<List<BigDecimal>> coordinates = new ArrayList<>();
    var allX =
        shapeAttributes.getAllPointsX().stream().map(x -> new BigDecimal(x.intValue())).toList();
    var allY =
        shapeAttributes.getAllPointsY().stream().map(y -> new BigDecimal(y.intValue())).toList();
    IntStream.range(0, allX.size())
        .forEach(i -> coordinates.add(List.of(allX.get(i), allY.get(i))));
    var featureId = randomUUID().toString();
    HashMap<String, Object> properties = new HashMap<>();
    properties.put("id", featureId);
    properties.put("zoom", zoom);
    return app.bpartners.geojobs.repository.model.Feature.builder()
        .id(featureId)
        .zoom(zoom)
        .geometry(
            app.bpartners.geojobs.repository.model.Feature.FeatureGeometry.builder()
                .geometryType(MULTI_POLYGON)
                .actualInstanceStringValue(
                    objectMapper()
                        .writeValueAsString(
                            new MultiPolygon()
                                .type(MultiPolygon.TypeEnum.MULTI_POLYGON)
                                .coordinates(List.of(List.of(coordinates)))))
                .build())
        .properties(properties)
        .build();
  }
}
