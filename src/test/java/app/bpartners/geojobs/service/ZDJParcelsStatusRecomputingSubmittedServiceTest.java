package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.status.ParcelDetectionStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.status.ZDJParcelsStatusRecomputingSubmitted;
import app.bpartners.geojobs.repository.ParcelDetectionTaskRepository;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.service.event.ZDJParcelsStatusRecomputingSubmittedService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ZDJParcelsStatusRecomputingSubmittedServiceTest {
  private static final String JOB_ID = "jobId";
  EventProducer eventProducerMock = mock();
  ParcelDetectionTaskRepository parcelDetectionTaskRepositoryMock = mock();
  ZDJParcelsStatusRecomputingSubmittedService subject =
      new ZDJParcelsStatusRecomputingSubmittedService(
          parcelDetectionTaskRepositoryMock, eventProducerMock);

  @Test
  void accept_ok() {
    when(parcelDetectionTaskRepositoryMock.findAllByJobId(JOB_ID))
        .thenReturn(
            List.of(
                ParcelDetectionTask.builder().asJobId("parcelJobId1").build(),
                ParcelDetectionTask.builder().asJobId("parcelJobId2").build()));

    assertDoesNotThrow(() -> subject.accept(new ZDJParcelsStatusRecomputingSubmitted(JOB_ID)));

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(2)).accept(listCaptor.capture());
    var events = listCaptor.getAllValues();
    var event1 = ((List<ParcelDetectionStatusRecomputingSubmitted>) events.getFirst()).getFirst();
    var event2 = ((List<ParcelDetectionStatusRecomputingSubmitted>) events.getLast()).getFirst();
    assertEquals(event1, new ParcelDetectionStatusRecomputingSubmitted("parcelJobId1"));
    assertEquals(event2, new ParcelDetectionStatusRecomputingSubmitted("parcelJobId2"));
  }
}
