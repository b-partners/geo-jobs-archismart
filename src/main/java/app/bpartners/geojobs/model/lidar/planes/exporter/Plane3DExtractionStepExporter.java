package app.bpartners.geojobs.model.lidar.planes.exporter;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.util.Collection;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Polygon;

@Builder(toBuilder = true)
@Slf4j
public class Plane3DExtractionStepExporter {
  private final ObjectMapper objectMapper;
  private final File directory;
  private final String crs;
  private final String suffix;
  private static final String GEO_JSON_FILE_EXTENSION = "geojson";

  public Plane3DExtractionStepExporter(
      ObjectMapper objectMapper, File directory, String crs, String suffix) {
    this.crs = crs;
    this.suffix = suffix;
    this.directory = directory;
    this.objectMapper = objectMapper;
  }

  public void export(Plane3DExtractionStep step, Collection<LasPointGeometry> points) {
    var features = objectMapper.createArrayNode();

    for (var p : points) {
      var coordinates = coordinates(p.getX(), p.getY(), p.getZ());
      var pointGeometry = geometry("Point", coordinates);
      var feature = feature(pointGeometry);

      features.add(feature);
    }

    write(step, featureCollection(features));
  }

  public void export(Plane3DExtractionStep step, Polygon polygon) {
    var features = objectMapper.createArrayNode();
    var coordinates = coordinates(polygon);
    var polygonGeometry = geometry("Polygon", coordinates);
    var feature = feature(polygonGeometry);
    features.add(feature);

    write(step, featureCollection(features));
  }

  private File toFile(Plane3DExtractionStep step) {
    return directory
        .toPath()
        .resolve(String.format("%s_%s.%s", step.toFilePrefix(), suffix, GEO_JSON_FILE_EXTENSION))
        .toFile();
  }

  private void write(Plane3DExtractionStep step, ObjectNode featureCollection) {
    try {
      var file = toFile(step);
      log.info("Exporting CityJSON step '{}' to file: {}", step, file.getAbsolutePath());

      objectMapper.writerWithDefaultPrettyPrinter().writeValue(toFile(step), featureCollection);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private ObjectNode crs() {
    var crsNode = objectMapper.createObjectNode();
    crsNode.put("type", "name");

    var props = objectMapper.createObjectNode();
    props.put("name", crs);
    crsNode.set("properties", props);

    return crsNode;
  }

  private ObjectNode featureCollection(ArrayNode features) {
    var root = objectMapper.createObjectNode();

    root.put("type", "FeatureCollection");
    root.set("crs", crs());
    root.set("features", features);

    return root;
  }

  private ObjectNode feature(ObjectNode geometry) {
    var feature = objectMapper.createObjectNode();

    feature.put("type", "Feature");
    feature.set("geometry", geometry);
    feature.set("properties", objectMapper.createObjectNode());

    return feature;
  }

  private ObjectNode geometry(String type, ArrayNode coordinates) {
    var geometry = objectMapper.createObjectNode();
    geometry.put("type", type);
    geometry.set("coordinates", coordinates);
    return geometry;
  }

  private ArrayNode coordinates(Polygon polygon) {
    var linearRing = objectMapper.createArrayNode();

    for (var coordinate : polygon.getCoordinates()) {
      var point = objectMapper.createArrayNode();
      point.add(coordinate.getX());
      point.add(coordinate.getY());
      point.add(coordinate.getZ());
      linearRing.add(point);
    }

    var coordinates = objectMapper.createArrayNode();
    coordinates.add(linearRing);
    return coordinates;
  }

  private ArrayNode coordinates(Double x, Double y, Double z) {
    var coordinates = objectMapper.createArrayNode();

    coordinates.add(x);
    coordinates.add(y);
    if (z != null) {
      coordinates.add(z);
    }

    return coordinates;
  }

  public Plane3DExtractionStepExporter subSuffix(String suffix) {
    var newSuffix = String.format("%s_%s", this.suffix, suffix);
    return this.toBuilder().suffix(newSuffix).build();
  }
}
