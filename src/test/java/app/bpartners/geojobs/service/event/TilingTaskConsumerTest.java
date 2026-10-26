package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.file.ExtensionGuesser;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.ImageValidator;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.zip.FileUnzipper;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class TilingTaskConsumerTest {
  private static final String LAYER = "Auvergne_Rhone_Alpes_PCRS_5cm";
  private final FileUnzipper fileUnzipper =
      new FileUnzipper(
          new FileWriter(new ObjectMapper(), new ExtensionGuesser()), mock(ImageValidator.class));

  @Test
  void successive_tasks_on_same_layer_only_get_their_own_tiles() {
    var downloadedDirectories = new ArrayList<File>();
    var subject =
        new TilingTaskConsumer(
            parcelContent -> {
              // the parcel id carries the tile path the fake tiler returns for this parcel
              var directory = fileUnzipper.apply(zipOf(parcelContent.getId()), LAYER).toFile();
              downloadedDirectories.add(directory);
              return directory;
            },
            mock(BucketComponent.class));
    var firstTask = taskWithTile("20/540884/376690");
    var secondTask = taskWithTile("20/540915/376636");

    subject.accept(firstTask);
    subject.accept(secondTask);

    assertEquals(List.of(coordinates(540884, 376690)), coordinatesOf(firstTask));
    assertEquals(List.of(coordinates(540915, 376636)), coordinatesOf(secondTask));
    assertEquals(LAYER + "/20/540915/376636.txt", secondTask.getTiles().getFirst().getBucketPath());
    assertNotEquals(downloadedDirectories.get(0), downloadedDirectories.get(1));
    downloadedDirectories.forEach(directory -> assertFalse(directory.exists()));
  }

  private static ParcelTilingTask taskWithTile(String tilePath) {
    return new ParcelTilingTask()
        .toBuilder()
            .parcels(
                List.of(
                    new Parcel()
                        .toBuilder()
                            .parcelContent(ParcelContent.builder().id(tilePath).build())
                            .build()))
            .build();
  }

  @SneakyThrows
  private static ZipFile zipOf(String tilePath) {
    var zip = Files.createTempFile("tiles", ".zip").toFile();
    try (var zos = new ZipOutputStream(new FileOutputStream(zip))) {
      zos.putNextEntry(new ZipEntry(tilePath + ".jpg"));
      zos.write("tile".getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();
    }
    return new ZipFile(zip);
  }

  private static List<TileCoordinates> coordinatesOf(ParcelTilingTask task) {
    return task.getTiles().stream().map(Tile::getCoordinates).toList();
  }

  private static TileCoordinates coordinates(int x, int y) {
    return new TileCoordinates().x(x).y(y).z(20);
  }
}
