package app.bpartners.geojobs.service.roofer3dbag.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Roofer3DBagCityJSONValidator implements Consumer<File> {
  private final ObjectMapper objectMapper;
  ;

  @Override
  public void accept(File file) {
    try {
      var root = objectMapper.readTree(file);

      var cityObjects = root.get("CityObjects");

      if (cityObjects == null || !cityObjects.isObject()) {
        throw new IllegalStateException("CityObjects missing or invalid");
      }

      if (cityObjects.size() < 2) {
        throw new IllegalStateException(
            "CityJSON must contain at least 2 CityObjects, found: " + cityObjects.size());
      }

    } catch (Exception e) {
      throw new IllegalStateException("Invalid CityJSON file", e);
    }
  }
}
