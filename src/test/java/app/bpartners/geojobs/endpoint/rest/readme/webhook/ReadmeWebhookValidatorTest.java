package app.bpartners.geojobs.endpoint.rest.readme.webhook;

import static app.bpartners.geojobs.endpoint.rest.readme.webhook.ReadmeWebhookValidator.calculateHmacSHA256;
import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.readme.webhook.model.CreateWebhook;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadmeWebhookValidatorTest {
  private static final String README_WEBHOOK_SECRET = "valid-secret";
  ReadmeWebhookConf readmeWebhookConfMock = mock();
  HttpServletRequest httpServletRequestMock = mock();
  ObjectMapper objectMapper = new ObjectMapper();
  ReadmeWebhookValidator subject = new ReadmeWebhookValidator(objectMapper, readmeWebhookConfMock);

  @BeforeEach
  void setUp() {
    when(readmeWebhookConfMock.getSecret()).thenReturn(README_WEBHOOK_SECRET);
  }

  @Test
  void accept_ok() throws JsonProcessingException, NoSuchAlgorithmException, InvalidKeyException {
    var createWebhook =
        CreateWebhook.builder().email("email").readmeProject("readmeProject").build();
    var signatureTime = now().toEpochMilli();
    setupRequest(createWebhook, README_WEBHOOK_SECRET, signatureTime);

    assertDoesNotThrow(
        () -> subject.accept(createWebhook, httpServletRequestMock, readmeWebhookConfMock));
  }

  @Test
  void throws_error_invalid_secret()
      throws JsonProcessingException, NoSuchAlgorithmException, InvalidKeyException {
    var createWebhook =
        CreateWebhook.builder().email("email").readmeProject("readmeProject").build();
    var signatureTime = now().toEpochMilli();
    setupRequest(createWebhook, "invalid-secret", signatureTime);

    var exception =
        assertThrows(
            ForbiddenException.class,
            () -> subject.accept(createWebhook, httpServletRequestMock, readmeWebhookConfMock));
    assertEquals("Webhook not valid", exception.getMessage());
  }

  @Test
  void throws_error_expired_time()
      throws JsonProcessingException, NoSuchAlgorithmException, InvalidKeyException {
    var createWebhook =
        CreateWebhook.builder().email("email").readmeProject("readmeProject").build();
    var signatureTime = now().minus(Duration.ofMinutes(35)).toEpochMilli();
    setupRequest(createWebhook, README_WEBHOOK_SECRET, signatureTime);

    var exception =
        assertThrows(
            ForbiddenException.class,
            () -> subject.accept(createWebhook, httpServletRequestMock, readmeWebhookConfMock));
    assertEquals("Webhook not valid", exception.getMessage());
  }

  private void setupRequest(CreateWebhook createWebhook, String secret, long time)
      throws JsonProcessingException, NoSuchAlgorithmException, InvalidKeyException {
    var v0 = time + "." + objectMapper.writeValueAsString(createWebhook);
    var readmeSignature = "t=" + time + ",v0=" + calculateHmacSHA256(v0, secret);
    when(httpServletRequestMock.getHeader(any())).thenReturn(readmeSignature);
  }
}
