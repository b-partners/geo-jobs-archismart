package app.bpartners.geojobs.service.cityjson.model.object;

import app.bpartners.geojobs.service.cityjson.model.object.io.CityJsonVisitor;
import app.bpartners.geojobs.service.cityjson.model.object.io.SurfaceAnnotators;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.*;
import lombok.SneakyThrows;

public class CityJsonIO {

  private static final ObjectMapper MAPPER =
      new ObjectMapper().disable(SerializationFeature.INDENT_OUTPUT);

  public static class Doc {
    public CityJSON header;
    public List<CityJSONFeature> features = new ArrayList<>();
  }

  public static Doc read(Path path) throws Exception {
    Doc doc = new Doc();
    try (var lines = Files.lines(path)) {
      List<String> all = lines.filter(s -> !s.isBlank()).collect(Collectors.toList());
      doc.header = MAPPER.readValue(all.get(0), CityJSON.class);
      for (int i = 1; i < all.size(); i++) {
        doc.features.add(MAPPER.readValue(all.get(i), CityJSONFeature.class));
      }
    }
    return doc;
  }

  @SneakyThrows
  public static void write(Doc doc, Path path) {
    List<String> out = new ArrayList<>();
    out.add(MAPPER.writeValueAsString(doc.header));
    for (CityJSONFeature f : doc.features) {
      out.add(MAPPER.writeValueAsString(f));
    }
    Files.write(path, out);
  }

  @SneakyThrows
  public static CityJsonIO.Doc computeAdditionalProperties(File originalCityJson) {
    CityJsonIO.Doc actual = CityJsonIO.read(originalCityJson.toPath());

    Consumer<CityJsonVisitor.SurfaceContext> pipeline =
        SurfaceAnnotators.slopeInDegrees()
            .andThen(SurfaceAnnotators.onlyOn("RoofSurface", SurfaceAnnotators.areaM2()))
            .andThen(SurfaceAnnotators.onlyOn("WallSurface", SurfaceAnnotators.areaM2()))
            .andThen(SurfaceAnnotators.wallHeightInMeters());

    CityJsonVisitor.forEachSurface(actual, pipeline);

    return actual;
  }

  @SneakyThrows
  public static File convertCityJsonSeqToCityJson(Path inputPath, Path outputPath) {
    ObjectNode cityJson = null;
    ArrayNode globalVertices = MAPPER.createArrayNode();
    ObjectNode globalCityObjects = MAPPER.createObjectNode();

    try (BufferedReader reader = Files.newBufferedReader(inputPath)) {
      String line;
      boolean firstLine = true;

      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty()) continue;

        JsonNode node = MAPPER.readTree(line);

        if (firstLine) {
          cityJson = (ObjectNode) node;

          if (cityJson.has("vertices") && cityJson.get("vertices").isArray()) {
            globalVertices.addAll((ArrayNode) cityJson.get("vertices"));
          }
          if (cityJson.has("CityObjects") && cityJson.get("CityObjects").isObject()) {
            globalCityObjects.setAll((ObjectNode) cityJson.get("CityObjects"));
          }
          firstLine = false;
        } else {
          int offset = globalVertices.size();

          JsonNode featVertices = node.get("vertices");
          if (featVertices != null && featVertices.isArray()) {
            globalVertices.addAll((ArrayNode) featVertices);
          }

          JsonNode featCityObjects = node.get("CityObjects");
          if (featCityObjects != null && featCityObjects.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = featCityObjects.fields();
            while (fields.hasNext()) {
              Map.Entry<String, JsonNode> entry = fields.next();
              ObjectNode cityObject = (ObjectNode) entry.getValue();

              // Décaler les indices dans chaque géométrie
              JsonNode geometries = cityObject.get("geometry");
              if (geometries != null && geometries.isArray()) {
                for (JsonNode geom : geometries) {
                  JsonNode boundaries = geom.get("boundaries");
                  if (boundaries != null) {
                    JsonNode shifted = shiftIndices(boundaries, offset);
                    ((ObjectNode) geom).set("boundaries", shifted);
                  }
                }
              }

              globalCityObjects.set(entry.getKey(), cityObject);
            }
          }
        }
      }
    }

    if (cityJson == null) {
      throw new IOException("Unable to read provided cityJSON file");
    }

    cityJson.set("CityObjects", globalCityObjects);
    cityJson.set("vertices", globalVertices);

    try (var writer = Files.newBufferedWriter(outputPath)) {
      MAPPER.writeValue(writer, cityJson);
    }
    return outputPath.toFile();
  }

  private static JsonNode shiftIndices(JsonNode node, int offset) {
    if (node.isInt()) {
      return MAPPER.getNodeFactory().numberNode(node.asInt() + offset);
    } else if (node.isArray()) {
      ArrayNode shifted = MAPPER.createArrayNode();
      for (JsonNode child : node) {
        shifted.add(shiftIndices(child, offset));
      }
      return shifted;
    }
    return node;
  }
}
