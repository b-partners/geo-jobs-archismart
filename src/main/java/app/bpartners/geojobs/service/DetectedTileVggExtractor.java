package app.bpartners.geojobs.service;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.model.geometry.VGG;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DetectedTileVggExtractor implements Function<DetectedTile, VGG> {
  private static final String POLYGON_SHAPE_NAME = "Polygon";
  private static final String LABEL_PROPERTY = "label";
  private static final String CONFIDENCE_PROPERTY = "confidence";
  private static final String ADDRESS_PROPERTY = "address";
  private static final String ADDRESSES_PROPERTY = "addresses";

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Override
  public VGG apply(DetectedTile detectedTile) {
    var vgg = new VGG();
    Map<String, VGG.Annotation.Region> regions = new HashMap<>();
    var addresses = getAddressesAndRegionsFromTile(detectedTile, regions);

    Map<String, Object> properties = new HashMap<>();
    if (!addresses.isEmpty()) {
      properties.put(ADDRESSES_PROPERTY, new ArrayList<>(addresses));
    }

    var filename = filenameOf(detectedTile);
    var annotation =
        VGG.Annotation.builder().filename(filename).properties(properties).regions(regions).build();

    vgg.put(filename, annotation);

    return vgg;
  }

  @NotNull
  private Set<String> getAddressesAndRegionsFromTile(
      DetectedTile detectedTile, Map<String, VGG.Annotation.Region> regions) {
    Set<String> addresses = new LinkedHashSet<>();

    var detectedObjects =
        detectedTile.getDetectedObjects() == null
            ? List.<DetectedObject>of()
            : detectedTile.getDetectedObjects();
    detectedObjects.forEach(
        detectedObject -> {
          var address = retrieveAddress(detectedObject);
          if (address != null) {
            addresses.add(address);
          }
          toRegions(detectedObject).forEach(region -> regions.put(randomUUID().toString(), region));
        });
    return addresses;
  }

  /**
   * Same as {@link #apply(DetectedTile)} but persists the VGG into a temporary file whose layout
   * ({@code [ {key: annotation} ]}) is the one {@link VggImageAnnotator} expects.
   */
  @SneakyThrows
  public File applyAsTempFile(DetectedTile detectedTile) {
    var vgg = apply(detectedTile);
    var tempFile =
        Files.createTempFile("tile-vgg-" + tileIdOf(detectedTile) + "-", ".json").toFile();
    tempFile.deleteOnExit();
    // VggImageAnnotator expects a JSON array of VGG objects, so wrap the single VGG in a list.
    objectMapper.writeValue(tempFile, List.of(vgg));
    return tempFile;
  }

  private List<VGG.Annotation.Region> toRegions(DetectedObject detectedObject) {
    var restFeature = detectedObject.getFeature();
    if (restFeature == null || restFeature.getGeometry() == null) {
      return List.of();
    }
    var multiPolygon = GeometryConverter.retrieveMultiPolygonFromFeature(restFeature, null);
    if (multiPolygon == null) {
      return List.of();
    }
    var label =
        detectedObject.getDetectableObjectType() == null
            ? null
            : detectedObject.getDetectableObjectType().name();
    var confidence = detectedObject.getComputedConfidence();

    var regions = new ArrayList<VGG.Annotation.Region>();
    for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
      if (multiPolygon.getGeometryN(i) instanceof Polygon polygon && !polygon.isEmpty()) {
        regions.add(toRegion(polygon, label, confidence));
      }
    }
    return regions;
  }

  private VGG.Annotation.Region toRegion(Polygon polygon, String label, Double confidence) {
    LineString exteriorRing = polygon.getExteriorRing();
    var allPointsX =
        Arrays.stream(exteriorRing.getCoordinates()).map(coordinate -> coordinate.x).toList();
    var allPointsY =
        Arrays.stream(exteriorRing.getCoordinates()).map(coordinate -> coordinate.y).toList();

    HashMap<String, Object> regionAttributes = new HashMap<>();
    if (label != null) {
      regionAttributes.put(LABEL_PROPERTY, label.toUpperCase());
    }
    if (confidence != null) {
      regionAttributes.put(CONFIDENCE_PROPERTY, confidence);
    }
    return VGG.Annotation.Region.builder()
        .regionAttribute(regionAttributes)
        .shapeAttribute(
            VGG.Annotation.Region.ShapeAttribute.builder()
                .name(POLYGON_SHAPE_NAME)
                .allPointsX(allPointsX)
                .allPointsY(allPointsY)
                .build())
        .build();
  }

  private String retrieveAddress(DetectedObject detectedObject) {
    var restFeature = detectedObject.getFeature();
    if (restFeature == null || restFeature.getProperties() == null) {
      return null;
    }
    var address = restFeature.getProperties().get(ADDRESS_PROPERTY);
    return address == null ? null : address.toString();
  }

  private String filenameOf(DetectedTile detectedTile) {
    var tile = detectedTile.getTile();
    var bucketPath = tile == null ? null : tile.getBucketPath();
    if (bucketPath != null) {
      int separatorIndex = bucketPath.lastIndexOf('/');
      return separatorIndex < 0 ? bucketPath : bucketPath.substring(separatorIndex + 1);
    }
    return tileIdOf(detectedTile) + ".png";
  }

  private String tileIdOf(DetectedTile detectedTile) {
    var tile = detectedTile.getTile();
    return tile == null ? null : tile.getId();
  }
}
