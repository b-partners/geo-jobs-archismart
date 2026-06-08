package app.bpartners.geojobs.service.cityjson.texture;

import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.cityjson.texture.model.TexturedCityJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CityJsonIOService {
  private final ObjectMapper objectMapper;
  @Deprecated private final GeometrySquareMeterArea geometrySquareMeterArea;

  public ObjectNode loadCityJson(File file) {
    try {
      return (ObjectNode) objectMapper.readTree(file);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read CityJSON file", e);
    }
  }

  public File toFile(TexturedCityJson texturedCityJson) {
    try {
      File file = Files.createTempFile("textured-cityjson-", ".json").toFile();

      try (FileWriter writer = new FileWriter(file)) {
        writer.write(texturedCityJson.json().toString());
      }

      return file;

    } catch (IOException e) {
      throw new IllegalStateException("Failed to write JSON to file", e);
    }
  }
}
