package app.bpartners.geojobs.utils.it;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the addresses to submit from a CSV whose header holds an {@code address} and a {@code
 * coordinates} column, the latter being a {@code "latitude, longitude"} pair, e.g. :
 *
 * <pre>
 * address,coordinates
 * "1 Rue Victor Prouvé, 54110 Dombasle-sur-Meurthe, France","48.609702, 6.351961"
 * </pre>
 *
 * <p>Columns are located by header name (case-insensitive), so their order does not matter, and
 * both fields are expected to be double-quoted since they embed commas.
 */
public final class AddressPointCsvReader {
  /** The KO detections shipped in the test resources, also replayed as 3D requests. */
  public static final String DETECTION_2D_KO_CSV = "detection/detection_2d_ko.csv";

  private static final String ADDRESS_HEADER = "address";
  private static final String COORDINATES_HEADER = "coordinates";

  private AddressPointCsvReader() {}

  /** Reads the given classpath resource, e.g. {@link #DETECTION_2D_KO_CSV}. */
  public static List<AddressPoint> readResource(String resourceName) throws IOException {
    try (InputStream in =
        AddressPointCsvReader.class.getClassLoader().getResourceAsStream(resourceName)) {
      if (in == null) {
        throw new IOException("Classpath resource not found: " + resourceName);
      }
      return parse(new String(in.readAllBytes(), UTF_8).lines().toList());
    }
  }

  /** Reads a CSV sitting outside the classpath, e.g. one passed through an env var. */
  public static List<AddressPoint> read(Path csvPath) throws IOException {
    return parse(Files.readAllLines(csvPath, UTF_8));
  }

  private static List<AddressPoint> parse(List<String> lines) throws IOException {
    if (lines.isEmpty()) {
      return List.of();
    }
    var headers = parseLine(lines.get(0));
    var addressIndex = indexOfHeader(headers, ADDRESS_HEADER);
    var coordinatesIndex = indexOfHeader(headers, COORDINATES_HEADER);

    var points = new ArrayList<AddressPoint>();
    for (int i = 1; i < lines.size(); i++) {
      var line = lines.get(i);
      if (line.isBlank()) {
        continue;
      }
      var values = parseLine(line);
      if (values.size() <= Math.max(addressIndex, coordinatesIndex)) {
        throw new IOException("Missing column on line " + (i + 1) + ": " + line);
      }
      points.add(
          toAddressPoint(values.get(addressIndex), values.get(coordinatesIndex), i + 1, line));
    }
    return List.copyOf(points);
  }

  /** Splits {@code "latitude, longitude"} into the two doubles of an {@link AddressPoint}. */
  private static AddressPoint toAddressPoint(
      String address, String coordinates, int lineNumber, String line) throws IOException {
    var parts = coordinates.split(",");
    if (parts.length != 2) {
      throw new IOException(
          "Expected 'latitude, longitude' coordinates on line " + lineNumber + ": " + line);
    }
    try {
      return new AddressPoint(
          address.trim(), Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()));
    } catch (IllegalArgumentException e) {
      throw new IOException("Cannot read line " + lineNumber + ": " + line, e);
    }
  }

  private static int indexOfHeader(List<String> headers, String header) throws IOException {
    for (int i = 0; i < headers.size(); i++) {
      if (headers.get(i).trim().equalsIgnoreCase(header)) {
        return i;
      }
    }
    throw new IOException("Missing '" + header + "' column, got headers: " + headers);
  }

  /**
   * Minimal RFC4180-ish parser: handles double-quoted fields with embedded commas and {@code ""}
   * escapes.
   */
  private static List<String> parseLine(String line) {
    var fields = new ArrayList<String>();
    var current = new StringBuilder();
    var inQuotes = false;
    for (int i = 0; i < line.length(); i++) {
      char ch = line.charAt(i);
      if (inQuotes) {
        if (ch == '"') {
          if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
            current.append('"');
            i++;
          } else {
            inQuotes = false;
          }
        } else {
          current.append(ch);
        }
      } else if (ch == '"') {
        inQuotes = true;
      } else if (ch == ',') {
        fields.add(current.toString().trim());
        current.setLength(0);
      } else {
        current.append(ch);
      }
    }
    fields.add(current.toString().trim());
    return fields;
  }

  /** Same as {@link #readResource(String)}, for use in stream pipelines and field initializers. */
  public static List<AddressPoint> readResourceUnchecked(String resourceName) {
    try {
      return readResource(resourceName);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
