package app.bpartners.geojobs.service.lidar.api;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Envelope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class OpenSourceLidarApi implements LidarApi {
  private final OpenSourceLidarApiConf conf;
  private final RestTemplate restTemplate;

  @Override
  @SuppressWarnings("all")
  public Set<String> apply(Envelope envelope) {
    var uriBuilder = UriComponentsBuilder.fromHttpUrl(conf.getUrl());
    conf.getDefaultParams(envelope).forEach(uriBuilder::queryParam);

    var features =
        requireNonNull(
                restTemplate
                    .getForEntity(uriBuilder.toUriString(), FeatureCollection.class)
                    .getBody())
            .getFeatures();

    return features.stream()
        .map(feature -> feature.getAssets().getUrl().orElseThrow())
        .collect(toSet());
  }
}
