package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.ThreeDRequestMonitoringTriggered;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.InternetAddress;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class ThreeDRequestMonitoringTriggeredService
    implements Consumer<ThreeDRequestMonitoringTriggered> {
  private static final String THREE_D_REQUEST_MONITORING_TRIGGERED_TEMPLATE =
      "three_d_request_monitoring_triggered";
  private final CityJSONRequestRepository cityJSONRequestRepository;
  private final Mailer mailer;
  private final HTMLTemplateParser htmlTemplateParser;
  private final String env = System.getenv("ENV");
  private final CommunityAuthorizationRepository communityAuthorizationRepository;

  @SneakyThrows
  @Override
  public void accept(ThreeDRequestMonitoringTriggered event) {
    var requestId = event.getRequestId();
    var communityOwnerId = event.getCommunityOwnerId();
    var communityOwner = communityAuthorizationRepository.findById(communityOwnerId).orElseThrow();

    var cityJSONRequest =
        cityJSONRequestRepository
            .findByIdAndCommunityOwnerId(requestId, communityOwnerId)
            .orElseThrow();
    var featuresWithDelimitation = cityJSONRequest.getFeaturesWithDelimitation();
    var delimitations =
        cityJSONRequest.getDelimitations().stream().map(FeatureMapper::toRestFeature).toList();
    var featuresProcessed =
        featuresWithDelimitation == null
            ? delimitations
            : featuresWithDelimitation.stream()
                .map(FeatureWithDelimitation::getRestFeature)
                .toList();

    mailer.accept(
        new Email(
            new InternetAddress("tech@birdia.fr"),
            List.of(),
            List.of(),
            "[geo-jobs/" + env + "] Requête 3D portant ID " + requestId,
            getHtmlBody(cityJSONRequest, communityOwner, featuresProcessed),
            List.of()));
  }

  private String getHtmlBody(
      CityJSONRequest request, CommunityAuthorization communityOwner, List<Feature> features) {
    var objectMapper = new ObjectMapper().findAndRegisterModules();
    var context = new Context();
    context.setVariable("request", request);
    context.setVariable(
        "features",
        features.stream()
            .map(
                feature -> {
                  try {
                    return new FeatureGeometryStringMap(
                        feature.getGeometry().getActualInstance().getClass().getSimpleName(),
                        objectMapper.writeValueAsString(feature.getGeometry()));
                  } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                  }
                }));
    context.setVariable("community", communityOwner);

    return htmlTemplateParser.apply(THREE_D_REQUEST_MONITORING_TRIGGERED_TEMPLATE, context);
  }

  private record FeatureGeometryStringMap(String geometryType, String geometryString) {}
}
