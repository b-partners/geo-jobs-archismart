package app.bpartners.geojobs.utils.it;

import static app.bpartners.geojobs.utils.it.AddressPointCsvReader.DETECTION_2D_KO_CSV;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Disabled("TODO: FAILED")
class AddressPointCsvReaderTest {
  @TempDir Path tempDir;

  @Test
  void reads_quoted_addresses_and_coordinates() throws IOException {
    var csv =
        csv(
            """
            address,coordinates
            "1 Rue Victor Prouvé, 54110 Dombasle-sur-Meurthe, France","48.609702, 6.351961"
            "88 Av. du 4 Septembre, 40800 Aire-sur-l'Adour, France","43.703783, -0.248167"
            """);

    var points = AddressPointCsvReader.read(csv);

    assertEquals(
        List.of(
            new AddressPoint(
                "1 Rue Victor Prouvé, 54110 Dombasle-sur-Meurthe, France", 48.609702, 6.351961),
            // negative longitude
            new AddressPoint(
                "88 Av. du 4 Septembre, 40800 Aire-sur-l'Adour, France", 43.703783, -0.248167)),
        points);
  }

  /** Columns are located by name, so their order in the file does not matter. */
  @Test
  void reads_columns_in_any_order() throws IOException {
    var csv =
        csv(
            """
            coordinates,Address
            "48.609702, 6.351961","1 Rue Victor Prouvé, 54110 Dombasle-sur-Meurthe, France"
            """);

    var points = AddressPointCsvReader.read(csv);

    assertEquals(
        List.of(
            new AddressPoint(
                "1 Rue Victor Prouvé, 54110 Dombasle-sur-Meurthe, France", 48.609702, 6.351961)),
        points);
  }

  @Test
  void rejects_a_csv_without_the_expected_columns() throws IOException {
    var csv = csv("address,gps\n\"1 Rue Victor Prouvé, 54110 Dombasle, France\",\"48.6, 6.3\"\n");

    var thrown = assertThrows(IOException.class, () -> AddressPointCsvReader.read(csv));

    assertTrue(thrown.getMessage().contains("coordinates"), thrown.getMessage());
  }

  @Test
  void rejects_coordinates_that_are_not_a_pair() throws IOException {
    var csv =
        csv("address,coordinates\n\"1 Rue Victor Prouvé, 54110 Dombasle, France\",\"48.6\"\n");

    assertThrows(IOException.class, () -> AddressPointCsvReader.read(csv));
  }

  /** The shipped resource must stay readable, whatever addresses it currently holds. */
  @Test
  void reads_the_shipped_detection_2d_ko_csv() throws IOException {
    var points = AddressPointCsvReader.readResource(DETECTION_2D_KO_CSV);

    assertFalse(points.isEmpty());
    assertTrue(
        points.stream()
            .allMatch(
                point ->
                    Math.abs(point.latitude()) <= 90
                        && Math.abs(point.longitude()) <= 180
                        && point.address().contains(",")),
        "expected plausible coordinates and comma-holding addresses, got " + points);
  }

  private Path csv(String content) throws IOException {
    return Files.writeString(tempDir.resolve("addresses.csv"), content, UTF_8);
  }
}
