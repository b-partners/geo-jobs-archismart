package app.bpartners.geojobs.service.detection;

import static java.util.UUID.randomUUID;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DetectionResponseAggregator
    implements Function<List<DetectionResponseAggregator.DetectionResponseUrl>, DetectionResponse> {

  @Override
  public DetectionResponse apply(List<DetectionResponseUrl> responseUrls) {
    if (responseUrls == null || responseUrls.isEmpty()) {
      return null;
    }

    Map<String, DetectionResponse.ImageData> aggregatedRstRaw = new HashMap<>();
    int regionCounter = 0;

    for (var r : responseUrls) {
      DetectionResponse response = r.detectionResponse();
      String actualUrl = r.apiUrl();
      if (response.getRstRaw() == null && response.getVegetationData() == null) continue;

      for (Map.Entry<String, DetectionResponse.ImageData> entry : response.getRstRaw().entrySet()) {
        DetectionResponse.ImageData imgData = entry.getValue();

        var imgKey = entry.getKey() + "_" + randomUUID();
        regionCounter =
            aggregateResponse(aggregatedRstRaw, imgKey, imgData, actualUrl, regionCounter);
      }
      if (response.getVegetationData() != null
          && !response.getVegetationData().getRegions().isEmpty()) {
        regionCounter =
            aggregateResponse(
                aggregatedRstRaw,
                "vegetation_data_" + randomUUID(),
                response.getVegetationData(),
                actualUrl,
                regionCounter);
      }
    }

    DetectionResponse aggregated = new DetectionResponse();
    aggregated.setSrcImageUrl(responseUrls.getFirst().detectionResponse().getSrcImageUrl());
    aggregated.setRstImageUrl(responseUrls.getFirst().detectionResponse().getRstImageUrl());
    aggregated.setRstRaw(aggregatedRstRaw);

    return aggregated;
  }

  private int aggregateResponse(
      Map<String, DetectionResponse.ImageData> aggregatedRstRaw,
      String imgKey,
      DetectionResponse.ImageData imgData,
      String actualUrl,
      int regionCounter) {
    DetectionResponse.ImageData aggregatedImgData =
        aggregateImageData(aggregatedRstRaw, imgKey, imgData);

    if (imgData.getRegions() != null) {
      for (DetectionResponse.ImageData.Region region : imgData.getRegions().values()) {
        region.getRegionAttributes().put("source_url", actualUrl);
        aggregatedImgData.getRegions().put("region_" + (++regionCounter), region);
      }
    }
    return regionCounter;
  }

  private DetectionResponse.ImageData aggregateImageData(
      Map<String, DetectionResponse.ImageData> aggregatedRstRaw,
      String imgKey,
      DetectionResponse.ImageData imgData) {
    return aggregatedRstRaw.computeIfAbsent(
        imgKey,
        k ->
            DetectionResponse.ImageData.builder()
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

  public record DetectionResponseUrl(DetectionResponse detectionResponse, String apiUrl) {}
}
