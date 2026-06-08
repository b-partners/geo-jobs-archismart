package app.bpartners.geojobs.service.lidar.api;

import static java.util.Objects.requireNonNull;

import app.bpartners.geojobs.file.FileWriter;
import java.io.File;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class LasIndexApi {
  private final LasIndexApiConf conf;
  private final RestTemplate restTemplate;

  @SuppressWarnings("all")
  public Optional<File> download(File lasFile, String fileUrl) {
    log.info("Start downloading lasIndex for FileURL={}", fileUrl);

    try {
      var directory = lasFile.getParentFile();
      var uri = UriComponentsBuilder.fromHttpUrl(conf.getLasIndexApiUrl()).build().toUriString();
      var body = Map.of("lidarUrl", fileUrl);
      var response = requireNonNull(restTemplate.postForEntity(uri, body, byte[].class));
      var data = requireNonNull(response.getBody());
      var outputPath = Paths.get(directory.getAbsolutePath(), getLasIndexFileName(lasFile));
      FileWriter.write(outputPath, data);

      log.info("Finished downloading lasIndex for FileURL={}", fileUrl);
      return Optional.of(outputPath.toFile());
    } catch (Exception e) {
      log.error("Cannot get LasIndexFile for fileUrl={}", fileUrl);
      return Optional.empty();
    }
  }

  private static String getLasIndexFileName(File lasFile) {
    return lasFile.getName().replaceAll(".las", ".lax").replaceAll(".laz", ".lax");
  }
}
