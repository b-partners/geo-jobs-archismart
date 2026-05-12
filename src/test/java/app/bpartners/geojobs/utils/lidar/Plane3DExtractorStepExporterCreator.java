package app.bpartners.geojobs.utils.lidar;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;

import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Plane3DExtractorStepExporterCreator {
  public static Plane3DExtractionStepExporter create() {
    return create(createTempDirectory());
  }

  public static Plane3DExtractionStepExporter create(File directory) {
    log.info("Output Folder = {}", directory.getAbsolutePath());

    return Plane3DExtractionStepExporter.builder()
        .suffix("")
        .crs("EPSG:2154")
        .objectMapper(new ObjectMapper())
        .directory(directory)
        .build();
  }
}
