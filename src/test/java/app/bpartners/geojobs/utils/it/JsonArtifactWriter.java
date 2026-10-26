package app.bpartners.geojobs.utils.it;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes the JSON artifacts an end-to-end run leaves behind (API responses, error bodies), always
 * carrying the {@code address} and {@code coordinates} of the {@link AddressPoint} they come from,
 * so a file found on disk tells which request produced it.
 */
public class JsonArtifactWriter {
  private final ObjectMapper objectMapper;

  public JsonArtifactWriter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /** Writes an API response object, pretty-printed and enriched. */
  public Path write(Path target, Object payload, AddressPoint point) throws IOException {
    return write(target, enrich(objectMapper.valueToTree(payload), point));
  }

  /**
   * Writes a raw response body — typically the body of a 4xx/5xx — pretty-printed and enriched. A
   * body that is not a JSON object (a bare error string, an array…) is first nested under a {@code
   * response} attribute so that the two attributes can hold the root.
   */
  public Path writeBody(Path target, String body, AddressPoint point) throws IOException {
    return write(target, enrich(parse(body), point));
  }

  /** Escapes what cannot appear in a file name, addresses being used as such. */
  public static String sanitize(String name) {
    return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
  }

  private ObjectNode enrich(JsonNode body, AddressPoint point) {
    ObjectNode root;
    if (body.isObject()) {
      root = (ObjectNode) body;
    } else {
      root = objectMapper.createObjectNode();
      root.set("response", body);
    }
    root.put("address", point.address());
    root.put("coordinates", point.coordinates());
    return root;
  }

  /** Parses the body, keeping it as a plain string node when it is not valid JSON. */
  private JsonNode parse(String body) {
    try {
      return objectMapper.readTree(body);
    } catch (IOException notJson) {
      return objectMapper.getNodeFactory().textNode(body);
    }
  }

  private Path write(Path target, JsonNode content) throws IOException {
    Files.createDirectories(target.getParent());
    Files.writeString(
        target, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(content), UTF_8);
    return target;
  }
}
