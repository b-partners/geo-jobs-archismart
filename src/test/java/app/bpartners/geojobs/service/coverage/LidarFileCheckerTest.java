package app.bpartners.geojobs.service.coverage;

import static app.bpartners.geojobs.service.coverage.LidarFileStatus.ABSENT;
import static app.bpartners.geojobs.service.coverage.LidarFileStatus.INVALID;
import static app.bpartners.geojobs.service.coverage.LidarFileStatus.UNKNOWN;
import static app.bpartners.geojobs.service.coverage.LidarFileStatus.VALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

class LidarFileCheckerTest {
  static final String URL = "https://lidar.test/file.laz";
  RestTemplate restTemplateMock = mock();
  LidarFileChecker subject = new LidarFileChecker(restTemplateMock);

  @SuppressWarnings("unchecked")
  private void serving(byte[] body, HttpHeaders requestHeaders) {
    when(restTemplateMock.execute(
            eq(URI.create(URL)), eq(HttpMethod.GET), any(), any(ResponseExtractor.class)))
        .thenAnswer(
            invocation -> {
              var request = mock(ClientHttpRequest.class);
              when(request.getHeaders()).thenReturn(requestHeaders);
              invocation.<RequestCallback>getArgument(2).doWithRequest(request);
              var response = mock(ClientHttpResponse.class);
              when(response.getBody()).thenReturn(new ByteArrayInputStream(body));
              return invocation.<ResponseExtractor<?>>getArgument(3).extractData(response);
            });
  }

  private static byte[] header(String signature, int size) {
    var bytes = new byte[size];
    var sig = signature.getBytes(StandardCharsets.US_ASCII);
    System.arraycopy(sig, 0, bytes, 0, Math.min(sig.length, size));
    return bytes;
  }

  @Test
  void check_is_valid_when_header_starts_with_las_signature_and_requests_only_the_header_range() {
    var requestHeaders = new HttpHeaders();
    serving(header("LASF", 227), requestHeaders);

    assertEquals(VALID, subject.check(URL));
    assertEquals("bytes=0-226", requestHeaders.getFirst(HttpHeaders.RANGE));
  }

  @Test
  void check_reads_at_most_the_header_when_server_ignores_range() {
    serving(header("LASF", 10_000), new HttpHeaders());

    assertEquals(VALID, subject.check(URL));
  }

  @Test
  void check_is_invalid_when_signature_is_wrong() {
    serving(header("<htm", 227), new HttpHeaders());

    assertEquals(INVALID, subject.check(URL));
  }

  @Test
  void check_is_invalid_when_header_is_truncated() {
    serving(header("LASF", 100), new HttpHeaders());

    assertEquals(INVALID, subject.check(URL));
  }

  @Test
  void check_is_absent_on_not_found() {
    when(restTemplateMock.execute(any(URI.class), any(), any(), any(ResponseExtractor.class)))
        .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "", null, null, null));

    assertEquals(ABSENT, subject.check(URL));
  }

  @Test
  void check_is_invalid_on_range_not_satisfiable_which_means_empty_file() {
    when(restTemplateMock.execute(any(URI.class), any(), any(), any(ResponseExtractor.class)))
        .thenThrow(
            HttpClientErrorException.create(
                HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, "", null, null, null));

    assertEquals(INVALID, subject.check(URL));
  }

  @Test
  void check_is_unknown_on_other_client_error() {
    when(restTemplateMock.execute(any(URI.class), any(), any(), any(ResponseExtractor.class)))
        .thenThrow(HttpClientErrorException.create(HttpStatus.FORBIDDEN, "", null, null, null));

    assertEquals(UNKNOWN, subject.check(URL));
  }

  @Test
  void check_is_unknown_on_server_error() {
    when(restTemplateMock.execute(any(URI.class), any(), any(), any(ResponseExtractor.class)))
        .thenThrow(mock(HttpServerErrorException.class));

    assertEquals(UNKNOWN, subject.check(URL));
  }

  @Test
  void check_is_unknown_on_network_error() {
    when(restTemplateMock.execute(any(URI.class), any(), any(), any(ResponseExtractor.class)))
        .thenThrow(new ResourceAccessException("timeout"));

    assertEquals(UNKNOWN, subject.check(URL));
  }

  @Test
  void check_is_unknown_on_malformed_url() {
    assertEquals(UNKNOWN, subject.check("not a url"));
  }

  @Test
  void check_is_invalid_when_response_has_no_body() {
    when(restTemplateMock.execute(any(URI.class), any(), any(), any(ResponseExtractor.class)))
        .thenAnswer(
            invocation -> {
              var response = mock(ClientHttpResponse.class);
              when(response.getBody()).thenReturn(null);
              return invocation.<ResponseExtractor<?>>getArgument(3).extractData(response);
            });

    assertEquals(INVALID, subject.check(URL));
  }

  @Test
  void check_is_invalid_when_extractor_yields_nothing() {
    when(restTemplateMock.execute(any(URI.class), any(), any(), any(ResponseExtractor.class)))
        .thenReturn(null);

    assertEquals(INVALID, subject.check(URL));
  }
}
