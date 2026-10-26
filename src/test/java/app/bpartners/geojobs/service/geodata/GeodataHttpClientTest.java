package app.bpartners.geojobs.service.geodata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.model.exception.GatewayTimeoutException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

class GeodataHttpClientTest {
  RestTemplate restTemplateMock = mock();
  GeodataHttpClient subject =
      new GeodataHttpClient(
          new ObjectMapper(), restTemplateMock, new GeodataApiConf("https://geodata.api", "key"));

  @Test
  void get_sends_api_key_and_query_params() {
    when(restTemplateMock.exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(String.class)))
        .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

    var actual = subject.get("/map/layers", Map.of("lat", 48.5), String.class);

    assertEquals("ok", actual);
    var uriCaptor = ArgumentCaptor.forClass(URI.class);
    var requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplateMock)
        .exchange(
            uriCaptor.capture(), eq(HttpMethod.GET), requestCaptor.capture(), eq(String.class));
    assertEquals("https://geodata.api/map/layers?lat=48.5", uriCaptor.getValue().toString());
    assertEquals("key", requestCaptor.getValue().getHeaders().getFirst("x-api-key"));
  }

  @Test
  void post_sends_api_key_and_json_content_type() {
    when(restTemplateMock.exchange(any(URI.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

    subject.post("/lidar/urls", "{}", String.class);

    var requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplateMock)
        .exchange(any(URI.class), eq(HttpMethod.POST), requestCaptor.capture(), eq(String.class));
    HttpHeaders headers = requestCaptor.getValue().getHeaders();
    assertEquals("key", headers.getFirst("x-api-key"));
    assertEquals("application/json", headers.getFirst(HttpHeaders.CONTENT_TYPE));
    assertEquals("{}", requestCaptor.getValue().getBody());
  }

  @Test
  void get_or_empty_returns_empty_on_resource_not_found_exception() {
    when(restTemplateMock.exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(String.class)))
        .thenThrow(notFoundWithBody("{\"type\":\"ResourceNotFoundException\",\"message\":\"m\"}"));

    assertTrue(
        subject.getOrEmptyWhenNotFound("/map/layers/actual", Map.of(), String.class).isEmpty());
  }

  @Test
  void get_throws_gateway_timeout_on_not_found() {
    when(restTemplateMock.exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(String.class)))
        .thenThrow(mock(HttpClientErrorException.NotFound.class));

    assertThrows(
        GatewayTimeoutException.class, () -> subject.get("/map/layers", Map.of(), String.class));
  }

  @Test
  void post_throws_gateway_timeout_on_server_error() {
    when(restTemplateMock.exchange(any(URI.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenThrow(mock(HttpServerErrorException.class));

    assertThrows(
        GatewayTimeoutException.class, () -> subject.post("/lidar/urls", "{}", String.class));
  }

  @Test
  void get_or_empty_throws_gateway_timeout_on_network_error() {
    when(restTemplateMock.exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(String.class)))
        .thenThrow(new ResourceAccessException("timeout"));

    assertThrows(
        GatewayTimeoutException.class,
        () -> subject.getOrEmptyWhenNotFound("/map/layers/actual", Map.of(), String.class));
  }

  @Test
  void get_throws_gateway_timeout_on_unexpected_status() {
    when(restTemplateMock.exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(String.class)))
        .thenReturn(new ResponseEntity<>("ok", HttpStatus.NO_CONTENT));

    assertThrows(
        GatewayTimeoutException.class, () -> subject.get("/map/layers", Map.of(), String.class));
  }

  @Test
  void post_throws_gateway_timeout_on_empty_body() {
    when(restTemplateMock.exchange(any(URI.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(new ResponseEntity<>(HttpStatus.OK));

    assertThrows(
        GatewayTimeoutException.class, () -> subject.post("/lidar/urls", "{}", String.class));
  }

  private static HttpClientErrorException.NotFound notFoundWithBody(String body) {
    return (HttpClientErrorException.NotFound)
        HttpClientErrorException.create(
            HttpStatus.NOT_FOUND, "Not Found", new HttpHeaders(), body.getBytes(), null);
  }

  @Test
  void get_or_empty_throws_gateway_timeout_on_not_found_without_geodata_error_body() {
    when(restTemplateMock.exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(String.class)))
        .thenThrow(notFoundWithBody("<html>Not Found</html>"));

    assertThrows(
        GatewayTimeoutException.class,
        () -> subject.getOrEmptyWhenNotFound("/map/layers/actual", Map.of(), String.class));
  }

  @Test
  void get_or_empty_throws_gateway_timeout_on_not_found_with_other_error_type() {
    when(restTemplateMock.exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(String.class)))
        .thenThrow(notFoundWithBody("{\"type\":\"NotAuthorizedException\"}"));

    assertThrows(
        GatewayTimeoutException.class,
        () -> subject.getOrEmptyWhenNotFound("/map/layers/actual", Map.of(), String.class));
  }

  @Test
  void get_or_empty_throws_gateway_timeout_on_not_found_with_empty_body() {
    when(restTemplateMock.exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(String.class)))
        .thenThrow(notFoundWithBody(""));

    assertThrows(
        GatewayTimeoutException.class,
        () -> subject.getOrEmptyWhenNotFound("/map/layers/actual", Map.of(), String.class));
  }

  @Test
  void get_throws_gateway_timeout_on_resource_not_found_exception() {
    when(restTemplateMock.exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(String.class)))
        .thenThrow(notFoundWithBody("{\"type\":\"ResourceNotFoundException\"}"));

    assertThrows(
        GatewayTimeoutException.class, () -> subject.get("/map/layers", Map.of(), String.class));
  }

  @Test
  void post_throws_gateway_timeout_on_resource_not_found_exception() {
    when(restTemplateMock.exchange(any(URI.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenThrow(notFoundWithBody("{\"type\":\"ResourceNotFoundException\"}"));

    assertThrows(
        GatewayTimeoutException.class, () -> subject.post("/lidar/urls", "{}", String.class));
  }
}
