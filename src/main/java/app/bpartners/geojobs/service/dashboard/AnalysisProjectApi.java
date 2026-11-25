package app.bpartners.geojobs.service.dashboard;

import static app.bpartners.geojobs.service.dashboard.ApiConfiguration.API_KEY_HEADER;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.web.util.DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY;

import app.bpartners.geojobs.service.dashboard.component.AnalysisProject;
import app.bpartners.geojobs.service.dashboard.component.Register2d;
import app.bpartners.geojobs.service.dashboard.component.Register3d;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Component
public class AnalysisProjectApi {
  private static final String PROJECT_PATH = "/users/{userId}/analysisProjects/{projectKey}";
  private static final String ANALYSIS_2D_PATH = PROJECT_PATH + "/2d";
  private static final String MODEL_3D_PATH = PROJECT_PATH + "/3d";
  private final RestTemplate restTemplate;
  private final ApiConfiguration apiConfiguration;
  private final SecurityApi securityApi;

  @Autowired
  public AnalysisProjectApi(ApiConfiguration apiConfiguration, SecurityApi securityApi) {
    this(apiConfiguration, securityApi, strictlyEncodingRestTemplate());
  }

  AnalysisProjectApi(
      ApiConfiguration apiConfiguration, SecurityApi securityApi, RestTemplate restTemplate) {
    this.apiConfiguration = apiConfiguration;
    this.securityApi = securityApi;
    this.restTemplate = restTemplate;
  }

  public AnalysisProject register2d(String apiKey, String projectKey, Register2d register2d) {
    return exchange(apiKey, PUT, ANALYSIS_2D_PATH, projectKey, register2d);
  }

  public AnalysisProject register3d(String apiKey, String projectKey, Register3d register3d) {
    return exchange(apiKey, PUT, MODEL_3D_PATH, projectKey, register3d);
  }

  public AnalysisProject get(String apiKey, String projectKey) {
    return exchange(apiKey, GET, PROJECT_PATH, projectKey, null);
  }

  private AnalysisProject exchange(
      String apiKey, HttpMethod method, String pathTemplate, String projectKey, Object body) {
    if (projectKey == null
        || projectKey.isBlank()
        || ".".equals(projectKey)
        || "..".equals(projectKey)) {
      throw new IllegalArgumentException("Invalid analysis project key");
    }
    var dashboardUserId = securityApi.retrieveUserId(apiKey);
    var headers = new HttpHeaders();
    headers.add(API_KEY_HEADER, apiKey);
    return restTemplate
        .exchange(
            apiConfiguration.getDashboardApiUrl() + pathTemplate,
            method,
            new HttpEntity<>(body, headers),
            AnalysisProject.class,
            dashboardUserId,
            projectKey)
        .getBody();
  }

  static RestTemplate strictlyEncodingRestTemplate() {
    var uriBuilderFactory = new DefaultUriBuilderFactory();
    uriBuilderFactory.setEncodingMode(VALUES_ONLY);
    var restTemplate = new RestTemplate();
    restTemplate.setUriTemplateHandler(uriBuilderFactory);
    return restTemplate;
  }
}
