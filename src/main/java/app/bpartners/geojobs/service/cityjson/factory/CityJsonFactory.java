package app.bpartners.geojobs.service.cityjson.factory;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;

import app.bpartners.geojobs.service.cityjson.exception.CityJsonException;
import app.bpartners.geojobs.service.cityjson.io.CityJsonWriter;
import app.bpartners.geojobs.service.cityjson.model.BuildingData;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CityJsonFactory {
  private final CityJsonWriter cityJsonWriter;
  private final File outputDirectory;

  @Autowired
  public CityJsonFactory() {
    this.cityJsonWriter = new CityJsonWriter();
    this.outputDirectory = createTempDirectory();
  }

  public CityJsonFactory(File outputDirectory) {
    this.cityJsonWriter = new CityJsonWriter();
    this.outputDirectory = outputDirectory;
  }

  public File make(String id, String title, List<BuildingData> data) throws CityJsonException {
    var filename = cityJsonFileName(id);
    var path = Path.of(outputDirectory.toString(), filename);
    var metadata = MetadataFactory.make(id, title);
    var model = CityModelFactory.make(data);

    cityJsonWriter.write(path, model, metadata);

    return path.toFile();
  }

  private static String cityJsonFileName(String id) {
    return String.format("%s.json", id);
  }
}
