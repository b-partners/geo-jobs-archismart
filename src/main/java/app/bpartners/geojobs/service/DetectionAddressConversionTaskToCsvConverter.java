package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;

import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.repository.model.DetectionAddressConversionTask;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectionAddressConversionTaskToCsvConverter
    implements BiFunction<List<DetectionAddressConversionTask>, String, File> {
  private final FileWriter fileWriter;

  @SneakyThrows
  @Override
  public File apply(List<DetectionAddressConversionTask> tasks, String fileName) {
    var builder = new StringBuilder();
    builder.append("Adresse\n");
    for (DetectionAddressConversionTask task : tasks) {
      if (task.getAddress() != null) {
        builder.append(task.getAddress()).append("\n");
      }
    }
    var csvAsString = builder.toString();
    var textFileGuessed =
        fileWriter.write(
            csvAsString.getBytes(StandardCharsets.UTF_8), createTempDirectory(), fileName);
    var filePathWithCsvExtension =
        textFileGuessed.getAbsolutePath().replaceAll("\\.[^.]+$", ".csv");
    var csvFileForced = new File(filePathWithCsvExtension);
    textFileGuessed.renameTo(csvFileForced);
    return csvFileForced;
  }
}
