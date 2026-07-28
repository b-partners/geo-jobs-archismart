package app.bpartners.geojobs.service.roofer3dbag.validator;

import static app.bpartners.geojobs.utils.ResourceFile.getResourceFile;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class Roofer3DBagCityJSONValidatorTest {
  private static final Roofer3DBagCityJSONValidator subject =
      new Roofer3DBagCityJSONValidator(new ObjectMapper());

  @Test
  void should_accept_cityjson_with_two_city_objects() {
    var file = getResourceFile("cityjson/valid_roofer_cityjson.jsonl");
    assertDoesNotThrow(() -> subject.accept(file));
  }

  @Test
  void should_throw_when_not_two_city_objects() {
    var file = getResourceFile("cityjson/invalid_roofer_cityjson.jsonl");
    assertThrows(IllegalStateException.class, () -> subject.accept(file));
  }
}
