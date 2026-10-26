package app.bpartners.geojobs.service.area.mutation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.service.area.mutation.model.MutationRequest;
import app.bpartners.geojobs.service.area.mutation.model.MutationResponse;
import app.bpartners.geojobs.service.area.mutation.model.MutationResponseStatus;
import app.bpartners.geojobs.service.area.mutation.model.MutationType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

class MutationApiTest {
  private static final String MOCK_MUTATION_API_URL = "https://mutation.api";
  ObjectMapper objectMapper = new ObjectMapper();
  RestTemplate restTemplateMock = mock();
  MutationApi subject = new MutationApi(objectMapper, restTemplateMock, MOCK_MUTATION_API_URL);

  @TempDir Path tempDir;

  @SneakyThrows
  @Test
  void detect_mutation_ok() {
    var beforeFile = writeFile("before.png", new byte[] {1, 2, 3});
    var afterFile = writeFile("after.png", new byte[] {4, 5, 6});
    var maskFile = writeFile("mask.png", new byte[] {7, 8, 9});
    var filename = "parcel-42.png";
    var expected =
        new MutationResponse(MutationResponseStatus.SUCCESS, MutationType.IMPROVEMENT, filename);

    when(restTemplateMock.postForEntity(any(String.class), any(), eq(MutationResponse.class)))
        .thenReturn(new ResponseEntity<>(expected, HttpStatus.OK));

    var actual = subject.detectMutation(beforeFile, afterFile, maskFile, filename);

    assertEquals(expected, actual);

    var requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplateMock)
        .postForEntity(
            eq(MOCK_MUTATION_API_URL + "/mutation"),
            requestCaptor.capture(),
            eq(MutationResponse.class));
    var actualPayload =
        objectMapper.readValue((String) requestCaptor.getValue().getBody(), MutationRequest.class);
    var expectedPayload =
        new MutationRequest(
            encodeBase64(beforeFile), encodeBase64(afterFile), encodeBase64(maskFile), filename);
    assertEquals(expectedPayload, actualPayload);
  }

  @SneakyThrows
  @Test
  void detect_mutation_throws_when_status_not_ok() {
    var beforeFile = writeFile("before.png", new byte[] {1, 2, 3});
    var afterFile = writeFile("after.png", new byte[] {4, 5, 6});
    var maskFile = writeFile("mask.png", new byte[] {7, 8, 9});
    var response =
        new MutationResponse(MutationResponseStatus.SUCCESS, MutationType.NONE, "parcel-42.png");

    when(restTemplateMock.postForEntity(any(String.class), any(), eq(MutationResponse.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.CREATED));

    assertThrows(
        IllegalStateException.class,
        () -> subject.detectMutation(beforeFile, afterFile, maskFile, "parcel-42.png"));
  }

  @SneakyThrows
  @Test
  void detect_mutation_throws_when_http_error() {
    var beforeFile = writeFile("before.png", new byte[] {1, 2, 3});
    var afterFile = writeFile("after.png", new byte[] {4, 5, 6});
    var maskFile = writeFile("mask.png", new byte[] {7, 8, 9});

    when(restTemplateMock.postForEntity(any(String.class), any(), eq(MutationResponse.class)))
        .thenThrow(mock(HttpClientErrorException.BadRequest.class));

    assertThrows(
        IllegalStateException.class,
        () -> subject.detectMutation(beforeFile, afterFile, maskFile, "parcel-42.png"));
  }

  private String encodeBase64(File file) throws IOException {
    return Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));
  }

  private File writeFile(String name, byte[] content) throws IOException {
    var path = tempDir.resolve(name);
    Files.write(path, content);
    return path.toFile();
  }
}
