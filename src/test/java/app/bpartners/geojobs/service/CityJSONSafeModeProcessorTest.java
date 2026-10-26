package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.model.lidar.LidarProcessorType;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CityJSONSafeModeProcessorTest {
  private CityJSONSafeModeProcessor subject;
  private CityJSONThreedProcessor internalProcessorMock;
  private CityJSON3DBagRooferProcessor rooferProcessorMock;
  private CityJSONRequestRepository cityJSONRequestRepositoryMock;

  @BeforeEach
  void setup() {
    internalProcessorMock = mock(CityJSONThreedProcessor.class);
    rooferProcessorMock = mock(CityJSON3DBagRooferProcessor.class);
    cityJSONRequestRepositoryMock = mock(CityJSONRequestRepository.class);

    when(cityJSONRequestRepositoryMock.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    subject =
        new CityJSONSafeModeProcessor(
            rooferProcessorMock, internalProcessorMock, cityJSONRequestRepositoryMock);
  }

  @Test
  void should_call_internal_processor_when_roofer_throws() {
    when(rooferProcessorMock.apply(any())).thenThrow(new RuntimeException());
    when(internalProcessorMock.apply(any())).thenReturn(mock());

    assertDoesNotThrow(() -> subject.apply(CityJSONRequest.builder().build()));
    verify(rooferProcessorMock, times(1)).apply(any());
    verify(internalProcessorMock, times(1)).apply(any());

    var captor = ArgumentCaptor.forClass(CityJSONRequest.class);
    verify(cityJSONRequestRepositoryMock, times(1)).save(captor.capture());
    assertEquals(LidarProcessorType.DEFAULT, captor.getValue().getLidarProcessorType());
  }

  @Test
  void should_not_call_internal_processor_when_roofer_is_success() {
    when(rooferProcessorMock.apply(any())).thenReturn(mock());

    assertDoesNotThrow(() -> subject.apply(CityJSONRequest.builder().build()));
    verify(internalProcessorMock, never()).apply(any());

    var captor = ArgumentCaptor.forClass(CityJSONRequest.class);
    verify(cityJSONRequestRepositoryMock, times(1)).save(captor.capture());
    assertEquals(LidarProcessorType.THREE_D_BAG_ROOFER, captor.getValue().getLidarProcessorType());
  }
}
