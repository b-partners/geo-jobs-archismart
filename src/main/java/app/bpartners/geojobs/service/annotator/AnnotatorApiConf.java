package app.bpartners.geojobs.service.annotator;

import app.bpartners.gen.annotator.endpoint.rest.client.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnnotatorApiConf {
  private final String annotatorApiKey;

  public AnnotatorApiConf(@Value("${annotator.api.key}") String annotatorApiKey) {
    this.annotatorApiKey = annotatorApiKey;
  }

  public ApiClient newApiClientWithApiKey() {
    var client = new ApiClient();
    client.setRequestInterceptor(
        httpRequestBuilder -> httpRequestBuilder.header("x-api-key", annotatorApiKey));
    return client;
  }
}
