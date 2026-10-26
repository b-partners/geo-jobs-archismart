package app.bpartners.geojobs.service.coverage;

import static app.bpartners.geojobs.service.coverage.LidarFileStatus.ABSENT;
import static app.bpartners.geojobs.service.coverage.LidarFileStatus.INVALID;
import static app.bpartners.geojobs.service.coverage.LidarFileStatus.UNKNOWN;
import static app.bpartners.geojobs.service.coverage.LidarFileStatus.VALID;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class LidarFileChecker {
  static final int LAS_PUBLIC_HEADER_SIZE = 227;
  static final String LAS_SIGNATURE = "LASF";
  private static final String HEADER_RANGE = "bytes=0-" + (LAS_PUBLIC_HEADER_SIZE - 1);

  private final RestTemplate restTemplate;

  public LidarFileChecker(
      @Qualifier(LidarFileRestTemplateConf.LIDAR_FILE_REST_TEMPLATE) RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public LidarFileStatus check(String fileUrl) {
    try {
      var header =
          restTemplate.execute(
              URI.create(fileUrl),
              GET,
              request -> request.getHeaders().set(HttpHeaders.RANGE, HEADER_RANGE),
              response -> readHeader(response.getBody()));
      return isLasHeader(header) ? VALID : INVALID;
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().isSameCodeAs(NOT_FOUND)) {
        return ABSENT;
      }
      if (e.getStatusCode().isSameCodeAs(REQUESTED_RANGE_NOT_SATISFIABLE)) {
        return INVALID;
      }
      log.warn("Lidar file {} could not be verified: {}", fileUrl, e.getMessage());
      return UNKNOWN;
    } catch (RestClientException | IllegalArgumentException e) {
      log.warn("Lidar file {} could not be verified: {}", fileUrl, e.getMessage());
      return UNKNOWN;
    }
  }

  private static byte[] readHeader(InputStream body) throws IOException {
    return body == null ? new byte[0] : body.readNBytes(LAS_PUBLIC_HEADER_SIZE);
  }

  private static boolean isLasHeader(byte[] header) {
    return header != null
        && header.length >= LAS_PUBLIC_HEADER_SIZE
        && LAS_SIGNATURE.equals(new String(header, 0, 4, StandardCharsets.US_ASCII));
  }
}
