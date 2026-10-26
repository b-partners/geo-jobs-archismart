package app.bpartners.geojobs.service.area.mutation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.service.area.mutation.model.AreaPictureHistoryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

class GeodataApiTest {
  private static final String MOCK_GEODATA_API_URL = "https://geodata.api";
  ObjectMapper objectMapper = new ObjectMapper();
  RestTemplate restTemplateMock = mock();
  GeodataApi subject = new GeodataApi(objectMapper, restTemplateMock, MOCK_GEODATA_API_URL);

  @SneakyThrows
  @Test
  void get_area_picture_history_ok() {
    var older =
        new AreaPictureHistoryResponse.DatedImage(
            2022, new AreaPictureHistoryResponse.PresignedUrl("https://geodata.test/old.jpg"));
    var mostRecent =
        new AreaPictureHistoryResponse.DatedImage(
            2024, new AreaPictureHistoryResponse.PresignedUrl("https://geodata.test/new.jpg"));
    var expected = new AreaPictureHistoryResponse(List.of(mostRecent, older));

    when(restTemplateMock.postForEntity(
            any(String.class), any(), eq(AreaPictureHistoryResponse.class)))
        .thenReturn(new ResponseEntity<>(expected, HttpStatus.OK));

    var actual = subject.getAreaPictureHistory(2.33, 48.87);

    assertEquals(expected, actual);

    var requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplateMock)
        .postForEntity(
            eq(MOCK_GEODATA_API_URL + "/areaPictureHistory"),
            requestCaptor.capture(),
            eq(AreaPictureHistoryResponse.class));
    var actualPayload =
        objectMapper.readValue((String) requestCaptor.getValue().getBody(), Map.class);
    assertEquals(Map.of("longitude", 2.33, "latitude", 48.87), actualPayload);
  }

  @Test
  void get_area_picture_history_throws_when_status_not_ok() {
    when(restTemplateMock.postForEntity(
            any(String.class), any(), eq(AreaPictureHistoryResponse.class)))
        .thenReturn(
            new ResponseEntity<>(new AreaPictureHistoryResponse(List.of()), HttpStatus.CREATED));

    assertThrows(IllegalStateException.class, () -> subject.getAreaPictureHistory(2.33, 48.87));
  }

  @Test
  void get_area_picture_history_throws_when_http_error() {
    when(restTemplateMock.postForEntity(
            any(String.class), any(), eq(AreaPictureHistoryResponse.class)))
        .thenThrow(mock(HttpClientErrorException.NotFound.class));

    assertThrows(IllegalStateException.class, () -> subject.getAreaPictureHistory(2.33, 48.87));
  }
}
