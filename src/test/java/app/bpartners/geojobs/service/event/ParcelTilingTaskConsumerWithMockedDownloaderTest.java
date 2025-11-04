package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.service.tiling.downloader.MockedTilesDownloader;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParcelTilingTaskConsumerWithMockedDownloaderTest {

  @Test
  void can_consume_with_no_error() {
    var subject = new TilingTaskConsumer(new MockedTilesDownloader(5_000, 0), mock());
    subject.accept(
        new ParcelTilingTask()
            .toBuilder()
                .parcels(
                    List.of(new Parcel().toBuilder().parcelContent(new ParcelContent()).build()))
                .build());
  }

  @Test
  void can_consume_with_some_errors() {
    var subject = new TilingTaskConsumer(new MockedTilesDownloader(2_000, 50), mock());

    try {
      for (int i = 0; i < 10; i++) {
        subject.accept(
            new ParcelTilingTask()
                .toBuilder()
                    .parcels(
                        List.of(
                            new Parcel().toBuilder().parcelContent(new ParcelContent()).build()))
                    .build());
      }
    } catch (Exception e) {
      return;
    }
    fail();
  }
}
