package app.bpartners.geojobs.service.cityjson.local;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.service.cityjson.local.CityJSONLocalTestWithoutSegmentationUtils.process;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.File;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@Slf4j
class CityJSONGenerationWithoutSegmentationTest {
  private static final File BASE_OUTPUT_FOLDER = createTempDirectory();

  @BeforeAll
  static void setup() {
    log.info("Output Folder = {}", BASE_OUTPUT_FOLDER.getAbsolutePath());
  }

  @Test
  void roof1() {
    assertDoesNotThrow(
        () ->
            process(
                "roof1",
                List.of("cityjson/geojson/pans/roof1/pan1.geojson"),
                Set.of("las/LHD_FXX_0644_6859_PTS_O_LAMB93_IGN69.copc.laz"),
                BASE_OUTPUT_FOLDER));
  }

  @Test
  void roof2() {
    assertDoesNotThrow(
        () ->
            process(
                "roof2",
                List.of(
                    "cityjson/geojson/pans/roof2/pan1.geojson",
                    "cityjson/geojson/pans/roof2/pan2.geojson"),
                Set.of("las/LHD_FXX_0644_6859_PTS_O_LAMB93_IGN69.copc.laz"),
                BASE_OUTPUT_FOLDER));
  }
}
