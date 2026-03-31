package app.bpartners.geojobs.endpoint.rest.controller.mapper.cityjson;

import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.model.geometry.area.Rate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoofScoreCategoryMapperTest {
  private final RoofScoreCategoryMapper subject = new RoofScoreCategoryMapper();

  @Test
  void mapping_integrity() {
    var domains = Rate.values();

    var actual = Arrays.stream(domains).map(subject::toRest).toArray();

    assertEquals(toStrings(domains), toStrings(actual));
  }

  private List<String> toStrings(Object[] objectList) {
    return Arrays.stream(objectList).map(Object::toString).toList();
  }
}
