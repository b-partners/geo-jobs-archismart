package app.bpartners.geojobs.service.gouv.fr.rnb.component.geometry;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeometryDeserializer extends JsonDeserializer<Geometry> {

  @Override
  public Geometry deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    JsonNode node = mapper.readTree(p);

    String type = node.get("type").asText();
    JsonNode coordsNode = node.get("coordinates");

    GeometryCoordinates coordinates =
        switch (type) {
          case "Point" -> {
            List<BigDecimal> point = mapper.convertValue(coordsNode, new TypeReference<>() {});
            yield new PointCoordinates(point);
          }
          case "Polygon" -> {
            List<List<List<BigDecimal>>> polygon =
                mapper.convertValue(coordsNode, new TypeReference<>() {});
            yield new PolygonCoordinates(polygon);
          }
          case "MultiPolygon" -> {
            List<List<List<List<BigDecimal>>>> multiPolygon =
                mapper.convertValue(coordsNode, new TypeReference<>() {});
            yield new MultiPolygonCoordinates(multiPolygon);
          }
          default -> throw new IllegalArgumentException("Unknown geojson type " + type);
        };
    var mappedType = app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.fromValue(type);
    return new Geometry(mappedType, coordinates);
  }
}
