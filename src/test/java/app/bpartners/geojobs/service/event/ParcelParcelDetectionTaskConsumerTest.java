package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.service.KeyPredicateFunction;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParcelParcelDetectionTaskConsumerTest {
  ParcelDetectionTaskConsumer subject =
      new ParcelDetectionTaskConsumer(mock(), mock(), mock(), new KeyPredicateFunction(), mock());

  @Test
  void consumes_task_without_parcels_ko() {
    assertThrows(
        IllegalArgumentException.class,
        () -> subject.accept(ParcelDetectionTask.builder().parcels(List.of()).build()));
  }

  @Test
  void consumes_task_without_tiles_ko() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            subject.accept(
                ParcelDetectionTask.builder()
                    .parcels(
                        List.of(
                            Parcel.builder()
                                .parcelContent(ParcelContent.builder().tiles(List.of()).build())
                                .build()))
                    .build()));

    assertThrows(
        IllegalArgumentException.class,
        () -> subject.accept(ParcelDetectionTask.builder().parcels(List.of()).build()));
  }
}
