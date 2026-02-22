package app.bpartners.geojobs.service.cityjson.local;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.service.cityjson.local.CityJSONLocalTestUtils.process;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.File;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@Slf4j
@EnabledIfEnvironmentVariable(named = "RUN_LIDAR_LOCAL_TESTS", matches = "true")
class CityJSONGenerationTest {
  private static final File BASE_OUTPUT_FOLDER = createTempDirectory();

  @BeforeAll
  static void setup() {
    log.info("Output Folder = {}", BASE_OUTPUT_FOLDER.getAbsolutePath());
  }

  @Test
  void roof1() {
    assertDoesNotThrow(() -> process("1.geojson", List.of("1_2_13.laz"), BASE_OUTPUT_FOLDER));
  }

  @Test
  void roof2() {
    assertDoesNotThrow(() -> process("2.geojson", List.of("1_2_13.laz"), BASE_OUTPUT_FOLDER));
  }

  @Test
  void roof3_chimney() {
    assertDoesNotThrow(
        () -> process("3_chimney.geojson", List.of("3_chimney.laz"), BASE_OUTPUT_FOLDER));
  }

  @Test
  void roof4() {
    assertDoesNotThrow(() -> process("4.geojson", List.of("4.laz"), BASE_OUTPUT_FOLDER));
  }

  @Test
  void roof5() {
    assertDoesNotThrow(() -> process("5.geojson", List.of("5.laz"), BASE_OUTPUT_FOLDER));
  }

  @Test
  void roof6() {
    assertDoesNotThrow(() -> process("6.geojson", List.of("6.laz"), BASE_OUTPUT_FOLDER));
  }

  @Test
  void roof7() {
    assertDoesNotThrow(() -> process("7.geojson", List.of("7.laz"), BASE_OUTPUT_FOLDER));
  }

  @Test
  void roof8() {
    assertDoesNotThrow(() -> process("8.geojson", List.of("8_9.laz"), BASE_OUTPUT_FOLDER));
  }

  @Test
  void roof9() {
    assertDoesNotThrow(() -> process("9.geojson", List.of("8_9.laz"), BASE_OUTPUT_FOLDER));
  }

  @Test
  void roof10() {
    assertDoesNotThrow(() -> process("10.geojson", List.of("10.laz"), BASE_OUTPUT_FOLDER));
  }

  @Test
  void roof11() {
    assertDoesNotThrow(
        () -> process("11_chimney.geojson", List.of("11_chimney.laz"), BASE_OUTPUT_FOLDER));
  }

  @Test
  void roof12() {
    assertDoesNotThrow(
        () -> process("12_chimney.geojson", List.of("12_chimney.laz"), BASE_OUTPUT_FOLDER));
  }

  @Test
  void roof13() {
    assertDoesNotThrow(() -> process("13.geojson", List.of("1_2_13.laz"), BASE_OUTPUT_FOLDER));
  }

  @Test
  void roof14() {
    assertDoesNotThrow(() -> process("14.geojson", List.of("14.laz"), BASE_OUTPUT_FOLDER));
  }

  @Test
  void roof15() {
    assertDoesNotThrow(() -> process("15.geojson", List.of("15.laz"), BASE_OUTPUT_FOLDER));
  }
}
