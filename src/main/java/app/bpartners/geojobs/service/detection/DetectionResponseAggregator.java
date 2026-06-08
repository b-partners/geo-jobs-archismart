package app.bpartners.geojobs.service.detection;

import static java.util.UUID.randomUUID;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DetectionResponseAggregator
    implements Function<
        List<DetectionResponseAggregator.DetectionResponseUrl>, DetectionResponseV2> {

  private static final String VEGETATION_KEY_PREFIX = "vegetation";

  @Override
  public DetectionResponseV2 apply(List<DetectionResponseUrl> responseUrls) {
    if (responseUrls == null || responseUrls.isEmpty()) {
      return null;
    }

    Map<String, DetectionResponseV2.ImageData> aggregatedImages = new HashMap<>();
    int regionCounter = 0;

    for (var r : responseUrls) {
      DetectionResponseV2 response = r.detectionResponse();
      String actualUrl = r.apiUrl();
      if (response == null || response.getImages() == null) {
        continue;
      }

      for (Map.Entry<String, DetectionResponseV2.ImageData> entry :
          response.getImages().entrySet()) {
        String key = entry.getKey();
        DetectionResponseV2.ImageData imgData = entry.getValue();

        if (isVegetation(key, imgData)) {
          // Vegetation is a dedicated image entry: aggregate it separately and skip it when it
          // carries no region, just like the legacy vegetation_data handling.
          if (imgData.getRegions() == null || imgData.getRegions().isEmpty()) {
            continue;
          }
          regionCounter =
              aggregateResponse(
                  aggregatedImages,
                  VEGETATION_KEY_PREFIX + "_data_" + randomUUID(),
                  imgData,
                  actualUrl,
                  regionCounter);
        } else {
          regionCounter =
              aggregateResponse(
                  aggregatedImages, key + "_" + randomUUID(), imgData, actualUrl, regionCounter);
        }
      }
    }

    return DetectionResponseV2.builder().images(aggregatedImages).build();
  }

  private boolean isVegetation(String key, DetectionResponseV2.ImageData imgData) {
    if (key != null && key.toLowerCase(Locale.ROOT).startsWith(VEGETATION_KEY_PREFIX)) {
      return true;
    }
    return imgData != null
        && imgData.getFilename() != null
        && imgData.getFilename().toLowerCase(Locale.ROOT).startsWith(VEGETATION_KEY_PREFIX);
  }

  private int aggregateResponse(
      Map<String, DetectionResponseV2.ImageData> aggregatedImages,
      String imgKey,
      DetectionResponseV2.ImageData imgData,
      String actualUrl,
      int regionCounter) {
    DetectionResponseV2.ImageData aggregatedImgData =
        aggregateImageData(aggregatedImages, imgKey, imgData);

    if (imgData.getRegions() != null) {
      for (DetectionResponseV2.ImageData.Region region : imgData.getRegions().values()) {
        region.getRegionAttributes().put("source_url", actualUrl);
        aggregatedImgData.getRegions().put("region_" + (++regionCounter), region);
      }
    }
    return regionCounter;
  }

  private DetectionResponseV2.ImageData aggregateImageData(
      Map<String, DetectionResponseV2.ImageData> aggregatedImages,
      String imgKey,
      DetectionResponseV2.ImageData imgData) {
    return aggregatedImages.computeIfAbsent(
        imgKey,
        k ->
            DetectionResponseV2.ImageData.builder()
                .fileref(imgData.getFileref())
                .size(imgData.getSize())
                .filename(imgData.getFilename())
                .base64ImgData(imgData.getBase64ImgData())
                .fileAttributes(
                    imgData.getFileAttributes() != null
                        ? new HashMap<>(imgData.getFileAttributes())
                        : new HashMap<>())
                .regions(new HashMap<>())
                .build());
  }

  public record DetectionResponseUrl(DetectionResponseV2 detectionResponse, String apiUrl) {}
}
