package app.bpartners.geojobs.utils;

import static java.lang.System.currentTimeMillis;

import app.bpartners.geojobs.unit.BuildingComparisonIT.BuildingResultComparison;
import java.io.*;
import java.nio.file.*;
import java.util.StringJoiner;
import lombok.SneakyThrows;

public class BuildingComparisonCSVLogger {
  private static final String CSV_FILE = "comparison_results_" + currentTimeMillis() + ".csv";
  private static final String HEADER =
      "adresse,intersection_rnb,diff_rnb,intersection_osm,diff_osm,intersection_google,diff_google";

  private final Path filePath;

  public BuildingComparisonCSVLogger(String outputPath) {
    this.filePath = Path.of(outputPath, CSV_FILE);
  }

  @SneakyThrows
  public void init() {

    // Crée le répertoire parent si nécessaire
    Files.createDirectories(filePath.getParent());

    // Écrit l'en-tête seulement si le fichier n'existe pas encore
    if (!Files.exists(filePath)) {
      Files.writeString(filePath, HEADER + System.lineSeparator());
    }
  }

  public void writeLine(String address, BuildingResultComparison comparison) throws IOException {
    StringJoiner joiner = new StringJoiner(",");

    joiner.add(escapeCsv(address));
    joiner.add(toStr(comparison.rnb().intersectionMatching()));
    joiner.add(toStr(comparison.rnb().differenceMatching()));
    joiner.add(toStr(comparison.osm().intersectionMatching()));
    joiner.add(toStr(comparison.osm().differenceMatching()));
    joiner.add(toStr(comparison.google().intersectionMatching()));
    joiner.add(toStr(comparison.google().differenceMatching()));

    Files.writeString(
        filePath, joiner.toString() + System.lineSeparator(), StandardOpenOption.APPEND);
  }

  private String toStr(Object value) {
    return value == null ? "null" : String.valueOf(value);
  }

  private String escapeCsv(String value) {
    if (value == null) return "";
    // Encadre de guillemets si la valeur contient une virgule, un guillemet ou un saut de ligne
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
