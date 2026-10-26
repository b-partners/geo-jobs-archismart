package app.bpartners.geojobs.validator;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;

import app.bpartners.geojobs.endpoint.rest.model.CreateDetection;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.service.DetectionImageZoomResolver;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateDetectionValidator implements Consumer<CreateDetection> {
  private static final String ZOOM_PROPERTY = "zoom";
  private static final int LEGACY_TOITURE_ZOOM = 20;

  private final DetectionImageZoomResolver detectionImageZoomResolver;

  @Override
  public void accept(CreateDetection createDetection) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (createDetection.getEmailReceiver() == null
        || createDetection.getEmailReceiver().isEmpty()) {
      exceptionMessageBuilder.append("CreateDetection.emailReceiver is mandatory. ");
    }
    if (createDetection.getZoneName() == null || createDetection.getZoneName().isEmpty()) {
      exceptionMessageBuilder.append("CreateDetection.zoneName is mandatory. ");
    }
    if ((createDetection.getDetectableObjectModel() == null
            || createDetection.getDetectableObjectModel().getModelName() == null)
        && (createDetection.getDetectableObjectModelList() == null
            || createDetection.getDetectableObjectModelList().isEmpty())) {
      exceptionMessageBuilder.append(
          "Either CreateDetection.detectableObjectModel or"
              + " CreateDetection.detectableObjectModelList is mandatory.");
    }
    var exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
    checkProvidedZoom(createDetection);
  }

  private void checkProvidedZoom(CreateDetection createDetection) {
    var featuresWithZoom =
        providedFeatures(createDetection).filter(CreateDetectionValidator::hasZoom).toList();
    if (featuresWithZoom.isEmpty()) {
      return;
    }
    if (!isToitureOnly(createDetection)
        || !featuresWithZoom.stream().allMatch(this::isLegacyZoom)) {
      throw new NotImplementedException(
          "Feature.properties.zoom cannot be provided, the zoom level is resolved from the"
              + " detectable objects.");
    }
    var resolvedZoom = detectionImageZoomResolver.toitureZoom();
    log.warn(
        "Deprecated: Feature.properties.zoom={} provided with BP_TOITURE, zoom resolved to {}."
            + " Providing the zoom will be rejected in a future version.",
        LEGACY_TOITURE_ZOOM,
        resolvedZoom);
    featuresWithZoom.forEach(
        feature -> {
          var properties = new HashMap<>(feature.getProperties());
          properties.put(ZOOM_PROPERTY, resolvedZoom);
          feature.setProperties(properties);
        });
  }

  private static Stream<Feature> providedFeatures(CreateDetection createDetection) {
    return createDetection.getGeoJsonZone() == null
        ? Stream.empty()
        : createDetection.getGeoJsonZone().stream().filter(Objects::nonNull);
  }

  private static boolean hasZoom(Feature feature) {
    return feature.getProperties() != null && feature.getProperties().get(ZOOM_PROPERTY) != null;
  }

  private boolean isLegacyZoom(Feature feature) {
    return feature.getProperties().get(ZOOM_PROPERTY) instanceof Number zoom
        && zoom.intValue() == LEGACY_TOITURE_ZOOM;
  }

  private static boolean isToitureOnly(CreateDetection createDetection) {
    var models =
        Stream.concat(
                Stream.ofNullable(createDetection.getDetectableObjectModel()),
                createDetection.getDetectableObjectModelList() == null
                    ? Stream.<DetectableObjectModel>empty()
                    : createDetection.getDetectableObjectModelList().stream())
            .filter(Objects::nonNull)
            .map(DetectableObjectModel::getModelName)
            .filter(Objects::nonNull)
            .toList();
    return !models.isEmpty() && models.stream().allMatch(TOITURE::equals);
  }
}
