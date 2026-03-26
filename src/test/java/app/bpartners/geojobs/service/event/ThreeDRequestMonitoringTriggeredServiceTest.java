package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.model.Feature.TypeEnum.FEATURE;
import static java.math.BigDecimal.ZERO;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.ThreeDRequestMonitoringTriggered;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import jakarta.mail.internet.InternetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class ThreeDRequestMonitoringTriggeredServiceTest {
  CityJSONRequestRepository cityJSONRequestRepositoryMock = mock();
  Mailer mailerMock = mock();
  HTMLTemplateParser htmlTemplateParser = new HTMLTemplateParser();
  CommunityAuthorizationRepository communityAuthorizationRepositoryMock = mock();
  ThreeDRequestMonitoringTriggeredService subject =
      new ThreeDRequestMonitoringTriggeredService(
          cityJSONRequestRepositoryMock,
          mailerMock,
          htmlTemplateParser,
          communityAuthorizationRepositoryMock);

  @SneakyThrows
  @Test
  void trigger_mail_ok() {
    String requestId = randomUUID().toString();
    String communityOwnerId = randomUUID().toString();
    CommunityAuthorization communityAuthorizationMock = mock(CommunityAuthorization.class);
    CityJSONRequest cityJSONRequestMock = mock(CityJSONRequest.class);
    FeatureWithDelimitation featureWithDelimitationMock = mock(FeatureWithDelimitation.class);
    when(communityAuthorizationMock.getId()).thenReturn(communityOwnerId);
    when(communityAuthorizationMock.getName()).thenReturn("community");
    when(communityAuthorizationMock.getEmail()).thenReturn("community@mail.com");
    when(cityJSONRequestMock.getId()).thenReturn(requestId);
    when(featureWithDelimitationMock.getRestFeature())
        .thenReturn(
            new Feature()
                .type(FEATURE)
                .geometry(new FeatureGeometry(new Point().coordinates(List.of(ZERO, ZERO))))
                .properties(new HashMap<>()));
    when(cityJSONRequestMock.getFeaturesWithDelimitation())
        .thenReturn(List.of(featureWithDelimitationMock));
    when(communityAuthorizationRepositoryMock.findById(communityOwnerId))
        .thenReturn(Optional.of(communityAuthorizationMock));
    when(cityJSONRequestRepositoryMock.findByIdAndCommunityOwnerId(requestId, communityOwnerId))
        .thenReturn(Optional.of(cityJSONRequestMock));

    assertDoesNotThrow(
        () -> subject.accept(new ThreeDRequestMonitoringTriggered(requestId, communityOwnerId)));

    verify(mailerMock)
        .accept(
            new Email(
                new InternetAddress("tech@birdia.fr"),
                List.of(),
                List.of(),
                "[geo-jobs/null] Requête 3D portant ID " + requestId,
                getHtmlBody(requestId, communityOwnerId),
                List.of()));
  }

  private static @NotNull String getHtmlBody(String requestId, String communityOwnerId) {
    return String.format(
        """
<html lang="fr">
<head>
    <title>3D request monitoring</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            background-color: #F1E4E7;
            color: #582d37;
            margin: 0;
            padding: 20px;
        }

        section {
            background-color: white;
            border-radius: 8px;
            box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
            padding: 20px;
            max-width: 600px;
            margin: auto;
            border-left: 6px solid rgba(171, 0, 86, 0.2);
        }

        p {
            font-size: 16px;
            line-height: 1.6;
            color: #660033;
        }

        ul {
            list-style-type: none;
            padding: 0;
        }

        ul li {
            background-color: rgba(0, 0, 0, 0.05);
            margin: 10px 0;
            padding: 10px;
            border-left: 4px solid rgba(171, 0, 86, 0.5);
            border-radius: 4px;
        }

        ul li span {
            font-weight: bold;
            color: rgba(122, 0, 61, 0.7);
        }

        ul li:not(:last-child) {
            margin-bottom: 10px;
        }

        h1 {
            font-size: 24px;
            color: rgba(171, 0, 86, 0.7);
            text-align: center;
        }
    </style>
</head>
<body>
<section>
    <h1>Informations de la requête 3D ID = <span>%s</span></h1>
    <p>Bonjour,</p>
    <p>Voici la liste des Feature correspondant à la requête du consommateur d'API <span>
            <span>community</span> ayant comme email <span>community@mail.com</span>
            et ID <span>%s</span>
        </span> :
    </p>
    <ul>
        <li>
            Type <span>Point</span> - Géometrie : <span>{&quot;coordinates&quot;:[0,0],&quot;type&quot;:null}</span>
        </li>
    </ul>
    <p>Cordialement.</p>
    <p>L'équipe BirdIA.</p>
</section>
</body>
</html>
""",
        requestId, communityOwnerId);
  }
}
