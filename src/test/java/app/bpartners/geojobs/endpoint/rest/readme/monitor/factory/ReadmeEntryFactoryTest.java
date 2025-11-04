package app.bpartners.geojobs.endpoint.rest.readme.monitor.factory;

import static java.net.http.HttpClient.Version.HTTP_2;
import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.readme.monitor.model.entry.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ReadmeEntryFactoryTest {
  HttpServletRequest request = mock();
  HttpServletResponse response = mock();
  private static final Instant REQUEST_STARTED_DATETIME = now();
  private static final Instant REQUEST_ENDED_DATETIME = REQUEST_STARTED_DATETIME.plusSeconds(10);
  private static final int RESPONSE_STATUS = 200;
  private static final String REQUEST_METHOD = "POST";
  private static final String HEADER_NAME = "authorization";
  private static final String HEADER_VALUE = "authorization-value";
  private static final String REQUEST_URI = "/requested/url";
  private static final String RESPONSE_CONTENT_TYPE = "application/json";
  private static final String REQUEST_URL = "https://dummy.com/requested/url";
  private static final Enumeration<String> HEADER_NAMES =
      new Vector<>(List.of(HEADER_NAME)).elements();

  ReadmeEntryFactory subject = new ReadmeEntryFactory();

  @BeforeEach
  void setup() {
    setupHttpServletRequest();
    setupHttpServletResponse();
  }

  @Test
  void can_create_entry() {
    var expected = expected();

    var actual =
        subject.createReadmeEntry(
            request, response, REQUEST_STARTED_DATETIME, REQUEST_ENDED_DATETIME);

    assertEquals(expected, actual);
  }

  private void setupHttpServletRequest() {
    when(request.getMethod()).thenReturn(REQUEST_METHOD);
    when(request.getRequestURI()).thenReturn(REQUEST_URI);
    when(request.getHeaderNames()).thenReturn(HEADER_NAMES);
    when(request.getHeader(HEADER_NAME)).thenReturn(HEADER_VALUE);
    when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));
    when(request.getParameterNames()).thenReturn(new Vector<String>().elements());
  }

  private void setupHttpServletResponse() {
    when(response.getHeaderNames()).thenReturn(new ArrayList<>(List.of(HEADER_NAME)));
    when(response.getStatus()).thenReturn(RESPONSE_STATUS);
    when(response.getHeader(HEADER_NAME)).thenReturn(HEADER_VALUE);
    when(response.getContentType()).thenReturn(RESPONSE_CONTENT_TYPE);
  }

  private ReadmeEntry expected() {
    var headers =
        List.of(ReadmeEntryHeader.builder().name(HEADER_NAME).value(HEADER_VALUE).build());

    return ReadmeEntry.builder()
        .pareRef(REQUEST_URI)
        .startedDateTime(REQUEST_STARTED_DATETIME)
        .time(Duration.between(REQUEST_STARTED_DATETIME, REQUEST_ENDED_DATETIME).toMillis())
        .request(
            ReadmeEntryRequest.builder()
                .headers(headers)
                .queryString(List.of())
                .httpVersion(HTTP_2.toString())
                .url(REQUEST_URL)
                .method(REQUEST_METHOD)
                .build())
        .response(
            ReadmeEntryResponse.builder()
                .headers(headers)
                .status(RESPONSE_STATUS)
                .statusText(HttpStatus.valueOf(RESPONSE_STATUS).name())
                .content(
                    ReadmeEntryResponseContent.builder().mimeType(RESPONSE_CONTENT_TYPE).build())
                .build())
        .build();
  }
}
