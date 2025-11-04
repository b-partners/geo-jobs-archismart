package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

import app.bpartners.geojobs.endpoint.event.model.readme.ReadmeLogCreated;
import app.bpartners.geojobs.endpoint.rest.readme.monitor.ReadmeMonitorConf;
import app.bpartners.geojobs.endpoint.rest.readme.monitor.model.ReadmeLog;
import app.bpartners.geojobs.model.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

class ReadmeLogCreatedServiceTest {
  @Mock ObjectMapper objectMapperMock;
  @InjectMocks ReadmeLogCreatedService subject;

  ReadmeLog readmeLogMock = mock();
  HttpResponse<Object> httpResponseMock = mock();
  ReadmeMonitorConf readmeMonitorConfMock = mock();
  HttpClient httpClientMock = mock();
  MockedStatic<HttpClient> httpClientStaticMock;

  @BeforeEach
  void setup() throws IOException, InterruptedException {
    openMocks(this);
    httpClientStaticMock = mockStatic(HttpClient.class);

    when(HttpClient.newHttpClient()).thenReturn(httpClientMock);
    when(readmeLogMock.development()).thenReturn(false);
    when(httpResponseMock.body()).thenReturn("[]");
    when(httpResponseMock.statusCode()).thenReturn(200);
    when(httpClientMock.send(any(), any())).thenReturn(httpResponseMock);
    when(objectMapperMock.writeValueAsString(List.of(readmeLogMock))).thenReturn("[]");
  }

  @Test
  void accept_http_request_ok() {
    var readmeLogCreated =
        ReadmeLogCreated.builder()
            .readmeMonitorConf(readmeMonitorConfMock)
            .readmeLog(readmeLogMock)
            .build();
    when(readmeMonitorConfMock.getUrl()).thenReturn("https://dummy.com");
    when(readmeMonitorConfMock.isDevelopment()).thenReturn(false);

    assertDoesNotThrow(() -> subject.accept(readmeLogCreated));
  }

  @Test
  void not_accept_if_development_is_different() {
    var readmeLogCreated =
        ReadmeLogCreated.builder()
            .readmeMonitorConf(readmeMonitorConfMock)
            .readmeLog(readmeLogMock)
            .build();
    when(readmeMonitorConfMock.getUrl()).thenReturn("https://dummy.com");
    when(readmeMonitorConfMock.isDevelopment()).thenReturn(true);

    var error = assertThrows(BadRequestException.class, () -> subject.accept(readmeLogCreated));

    assertEquals("readmeLog.development should be true", error.getMessage());
  }

  @AfterEach
  void cleanup() {
    httpClientStaticMock.close();
  }
}
