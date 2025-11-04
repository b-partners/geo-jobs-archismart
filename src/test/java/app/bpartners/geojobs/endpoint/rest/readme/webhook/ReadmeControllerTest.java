package app.bpartners.geojobs.endpoint.rest.readme.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.rest.controller.ReadmeController;
import app.bpartners.geojobs.endpoint.rest.readme.webhook.model.SingleUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadmeControllerTest {
  private final ReadmeWebhookValidator readmeWebhookValidatorMock = mock();
  private final ReadmeWebhookService readmeWebhookServiceMock = mock();
  SingleUserInfo expected = mock();
  ReadmeController subject =
      new ReadmeController(
          readmeWebhookValidatorMock, readmeWebhookServiceMock, mock(ReadmeWebhookConf.class));

  @BeforeEach
  void setup() {
    doNothing().when(readmeWebhookValidatorMock).accept(any(), any(), any());
    when(readmeWebhookServiceMock.retrieveUserInfoByEmail(any())).thenReturn(expected);
  }

  @Test
  void readme_webhook_ok() {
    var actual = subject.readmeWebhook(mock(), mock());

    assertEquals(expected, actual);
  }
}
