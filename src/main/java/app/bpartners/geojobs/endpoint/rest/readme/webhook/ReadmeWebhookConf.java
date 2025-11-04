package app.bpartners.geojobs.endpoint.rest.readme.webhook;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@NoArgsConstructor
@ConfigurationProperties("readme.webhook")
public class ReadmeWebhookConf {
  private String secret;
}
