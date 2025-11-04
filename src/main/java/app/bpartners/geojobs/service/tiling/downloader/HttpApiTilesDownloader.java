package app.bpartners.geojobs.service.tiling.downloader;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.nio.file.Files.createTempFile;

import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.zip.FileUnzipper;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.model.ParcelContent;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@ConditionalOnProperty(value = "tiles.downloader.mock.activated", havingValue = "false")
public class HttpApiTilesDownloader implements TilesDownloader {
  private final ObjectMapper om;
  private final String tilesDownloaderApiURl;
  private final FileWriter fileWriter;
  private final FileUnzipper fileUnzipper;

  public HttpApiTilesDownloader(
      @Value("${tiles.downloader.api.url}") String tilesDownloaderApiURl,
      ObjectMapper om,
      FileWriter fileWriter,
      FileUnzipper fileUnzipper) {
    this.om = om;
    this.tilesDownloaderApiURl = tilesDownloaderApiURl;
    this.fileWriter = fileWriter;
    this.fileUnzipper = fileUnzipper;
  }

  @Override
  public File apply(ParcelContent parcelContent) {
    RestTemplate restTemplate = new RestTemplate();
    MultipartBodyBuilder bodies = new MultipartBodyBuilder();
    bodies.part("server", new FileSystemResource(getServerInfoFile(parcelContent)));
    bodies.part("geojson", getGeojson(parcelContent));
    MultiValueMap<String, HttpEntity<?>> multipartBody = bodies.build();
    HttpEntity<MultiValueMap<String, HttpEntity<?>>> request = new HttpEntity<>(multipartBody);
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromHttpUrl(tilesDownloaderApiURl)
            .path("/geo-tiles")
            .queryParam("zoom_size", parcelContent.getFeature().getZoom());

    ResponseEntity<TilerResponse> responseEntity;
    try {
      responseEntity =
          restTemplate.postForEntity(builder.toUriString(), request, TilerResponse.class);
    } catch (RestClientException e) {
      log.error("Error downloading tiles : {}", e.getMessage());
      throw new ApiException(SERVER_EXCEPTION, e);
    }
    if (responseEntity.getStatusCode().value() == 200 && responseEntity.getBody() != null) {
      try {
        ResponseEntity<byte[]> file =
            restTemplate.getForEntity(new URI(responseEntity.getBody().fileUrl), byte[].class);
        var zip = fileWriter.apply(file.getBody(), null);
        var layers =
            parcelContent.getFeature().getProperties().get("priorityLayer") == null
                ? parcelContent.getGeoServerParameter().getLayers()
                : parcelContent.getFeature().getProperties().get("priorityLayer").toString();
        var unzipped = unzip(zip, layers);
        zip.delete();

        return unzipped;
      } catch (IOException | URISyntaxException e) {
        log.error("Error zipping downloaded tiles : {}", e.getMessage());
        throw new RuntimeException(e);
      }
    }
    throw new ApiException(SERVER_EXCEPTION, "Server error");
  }

  private File unzip(File downloadedTiles, String layer) throws IOException {
    ZipFile asZipFile = new ZipFile(downloadedTiles);
    Path unzippedPath = fileUnzipper.apply(asZipFile, layer);
    return unzippedPath.toFile();
  }

  private record TilerResponse(@JsonProperty("file_url") String fileUrl) {}

  @SneakyThrows
  private File getServerInfoFile(ParcelContent parcelContent) {
    var geoServerParameter = parcelContent.getGeoServerParameter();
    String geoServerUrl = String.valueOf(parcelContent.getGeoServerUrl());
    String service = geoServerParameter.getService();
    String request = geoServerParameter.getRequest();
    String layers =
        parcelContent.getFeature().getProperties().get("priorityLayer") == null
            ? geoServerParameter.getLayers()
            : parcelContent.getFeature().getProperties().get("priorityLayer").toString();
    String styles = geoServerParameter.getStyles();
    String format = geoServerParameter.getFormat();
    String transparent = String.valueOf(geoServerParameter.getTransparent());
    String version = geoServerParameter.getVersion();
    String width = String.valueOf(geoServerParameter.getWidth());
    String height = String.valueOf(geoServerParameter.getHeight());
    String srs = geoServerParameter.getSrs();

    Map<String, Object> serverInfo = new HashMap<>();
    serverInfo.put("url", geoServerUrl);

    Map<String, Object> serverParameter = new HashMap<>();
    serverParameter.put("service", service);
    serverParameter.put("request", request);
    serverParameter.put("layers", layers);
    serverParameter.put("styles", styles);
    serverParameter.put("format", format);
    serverParameter.put("transparent", transparent);
    serverParameter.put("version", version);
    serverParameter.put("width", width);
    serverParameter.put("height", height);
    serverParameter.put("srs", srs);

    serverInfo.put("parameter", serverParameter);
    serverInfo.put("concurrency", 1);

    Path serverInfoPath = createTempFile(tempFileParcelPrefix(parcelContent) + "_server", "json");
    File file = serverInfoPath.toFile();
    om.writeValue(file, serverInfo);

    return file;
  }

  @SneakyThrows
  private FileSystemResource getGeojson(ParcelContent parcelContent) {
    Map<String, Object> feature = new HashMap<>();
    feature.put("type", "Feature");
    feature.put("geometry", parcelContent.restFeatures().getGeometry());

    var featuresList = new ArrayList<>();
    featuresList.add(feature);

    Map<String, Object> featureCollection = new HashMap<>();
    featureCollection.put("type", "FeatureCollection");
    featureCollection.put("features", featuresList);

    Path geojsonPath = createTempFile(tempFileParcelPrefix(parcelContent), "geojson");
    File geojsonFile = geojsonPath.toFile();
    om.writeValue(geojsonFile, featureCollection);

    return new FileSystemResource(geojsonFile);
  }

  private static String tempFileParcelPrefix(ParcelContent parcelContent) {
    return "parcel_" + parcelContent.getId();
  }
}
