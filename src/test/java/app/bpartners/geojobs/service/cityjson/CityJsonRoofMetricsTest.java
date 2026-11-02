package app.bpartners.geojobs.service.cityjson;

import static app.bpartners.geojobs.utils.ResourceFile.getResourceFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.service.cityjson.model.object.CityJsonIO;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class CityJsonRoofMetricsTest {
  // 10m x 10m building, ground at z=100, one roof plane going from z=105 (y=0) to z=110 (y=10),
  // plus a flat LoD 1.2 roof at z=120 that must be ignored in favour of the LoD 2.2 one
  private static final String SHED_ROOF_CITYJSON =
      """
{
  "type": "CityJSON",
  "version": "2.0",
  "transform": {"scale": [0.01, 0.01, 0.01], "translate": [1000, 2000, 0]},
  "vertices": [
    [0, 0, 10000], [1000, 0, 10000], [1000, 1000, 10000], [0, 1000, 10000],
    [0, 0, 10500], [1000, 0, 10500], [1000, 1000, 11000], [0, 1000, 11000],
    [-1000, -1000, 12000], [2000, -1000, 12000], [2000, 2000, 12000], [-1000, 2000, 12000]
  ],
  "CityObjects": {
    "building-0": {
      "type": "Building",
      "geometry": [
        {
          "type": "MultiSurface",
          "lod": "1.2",
          "boundaries": [[[8, 9, 10, 11]]],
          "semantics": {"surfaces": [{"type": "RoofSurface"}], "values": [0]}
        },
        {
          "type": "Solid",
          "lod": "2.2",
          "boundaries": [[
            [[0, 3, 2, 1]], [[4, 5, 6, 7]],
            [[0, 1, 5, 4]], [[1, 2, 6, 5]], [[2, 3, 7, 6]], [[3, 0, 4, 7]]
          ]],
          "semantics": {
            "surfaces": [{"type": "GroundSurface"}, {"type": "RoofSurface"}, {"type": "WallSurface"}],
            "values": [[0, 1, 2, 2, 2, 2]]
          }
        }
      ]
    }
  }
}
""";

  @Test
  void reads_slope_and_height_from_most_detailed_lod() {
    var actual = CityJsonRoofMetrics.from(List.of(write(SHED_ROOF_CITYJSON))).orElseThrow();

    assertEquals(26.57, actual.slopeInDegrees());
    assertEquals(10.0, actual.heightInMeters());
  }

  @Test
  void empty_without_roof_surface() {
    var cityJson =
        """
        {"type": "CityJSON", "version": "2.0", "vertices": [], "CityObjects": {}}
        """;

    var actual = CityJsonRoofMetrics.from(List.of(write(cityJson)));

    assertTrue(actual.isEmpty());
  }

  @SneakyThrows
  @Test
  void reads_roofer_generated_cityjson() {
    var cityJsonSeq = getResourceFile("cityjson/valid_roofer_cityjson.jsonl");
    var cityJson =
        CityJsonIO.convertCityJsonSeqToCityJson(
            cityJsonSeq.toPath(), File.createTempFile("roofer", ".json").toPath());

    var actual = CityJsonRoofMetrics.from(List.of(cityJson)).orElseThrow();

    assertTrue(actual.slopeInDegrees() >= 0 && actual.slopeInDegrees() <= 90);
    assertTrue(actual.heightInMeters() > 0);
  }

  @SneakyThrows
  private static File write(String content) {
    var file = File.createTempFile("cityjson", ".json");
    Files.writeString(file.toPath(), content);
    file.deleteOnExit();
    return file;
  }
}
