package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CityJSONSafeModeProcessorTest {
  private CityJSONSafeModeProcessor subject;
  private CityJSONInternalProcessor internalProcessorMock;
  private CityJSON3DBagRooferProcessor rooferProcessorMock;

  @BeforeEach
  void setup() {
    internalProcessorMock = mock(CityJSONInternalProcessor.class);
    rooferProcessorMock = mock(CityJSON3DBagRooferProcessor.class);

    subject = new CityJSONSafeModeProcessor(rooferProcessorMock, internalProcessorMock);
  }

  @Test
  void should_call_internal_processor_when_roofer_throws() {
    when(rooferProcessorMock.apply(any())).thenThrow(new RuntimeException());
    when(internalProcessorMock.apply(any())).thenReturn(mock());

    assertDoesNotThrow(() -> subject.apply(any()));
    verify(rooferProcessorMock, times(1)).apply(any());
    verify(internalProcessorMock, times(1)).apply(any());
  }

  @Test
  void should_not_call_internal_processor_when_roofer_is_success() {
    when(rooferProcessorMock.apply(any())).thenReturn(mock());

    assertDoesNotThrow(() -> subject.apply(any()));
    verify(internalProcessorMock, never()).apply(any());
  }
}
