package app.bpartners.geojobs.service.roofer3dbag;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.geojobs.service.roofer3dbag.conf.RooferApiProperties;
import app.bpartners.geojobs.service.roofer3dbag.model.CityJsonGenerationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Disabled("TODO: local use only")
class Roofer3DBagApiClientIT {
  private final String BASE_URL = System.getenv("ROOFER_3D_BAG_BASE_URL");
  private final String API_KEY = System.getenv("ROOFER_3D_BAG_API_KEY");
  private final String ROOFER_3_D_BAG_COMPLEXITY_FACTOR =
      System.getenv("ROOFER_3D_BAG_COMPLEXITY_FACTOR");
  Roofer3DBagApiClient subject =
      new Roofer3DBagApiClient(
          getRestTemplate(),
          new RooferApiProperties(BASE_URL, API_KEY),
          new ObjectMapper(),
          Float.valueOf(ROOFER_3_D_BAG_COMPLEXITY_FACTOR));

  @Test
  void convert_lidar_to_city_json() {
    var geoJsonBuildingPresignedUrl =
        "https://3dbag-dataset.s3.eu-west-3.amazonaws.com/emprises-2154/paris.geojson?response-content-disposition=inline&X-Amz-Content-Sha256=UNSIGNED-PAYLOAD&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEP%2F%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCWV1LXdlc3QtMyJHMEUCIHO8Z8EFQe3Xiz45rPSwLAmhTxQ1GCnt5Jfqf1Dev7R5AiEA7bae%2FhB6bt%2BEye3AnjdM8zQCVtzF5hEZUFF3PdODfUIq1wMIyP%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FARAAGgwyMDUyMDUxMDE0MDkiDBUwZnVm9E0uX1IvNiqrA8aC17zB8sKEADWW3ve68ip5HYuPhbXPyjbthz6I3ZVuLLLcuWa4HP12MKXZobT8KUoQ7ppp9CxO73uC0e7FblbBAT2m1d6os0T6hhZfpSDFvNHVtDiA6d%2F6sIhjPqJ9uD97xP%2Fc0fgBzjb3BLf8iW%2FDbAbaFuTGt9%2FKq9P%2FMkc32TLMAdUS4HnYVenARWatuztd6YwucHsprjQD4re%2BrWvM9nMRcvxn%2BveN%2BI8VjKN6NaFNNMG4RiTB10K9yMSNMxVTd0SyVsGpCsl8UFmLT511MTgygoA3ZvKpx%2BwCW1dwyJI4iaC4eU3M4rZq60b6PWxT1We3I0489prj%2FKsRjYqI2ILH3aDc7FnNJhexPi%2F9xwcioC%2B0pbxAtg18u05ep3kVxyQVA%2FTyVsa6RuD%2BITgyv1KNCIEsU906Ln9iI6H6DZ1MXnzgtyj8b73PkunOhVV8iHRrvo4zF6OuACynaAOClx6lgvsaRnB1lvhUQ3XQ9gbxL%2FppDi7kAIRTEO4EfeQeJ7dpDNI909%2BstlzxVsiAycLkMRfIbuAtD3IiAkloFCnSN13YXuCNfIAwmYX2zwY63gLbBStQkuWG4K7yXVx2f%2BuvBfvvo4t68Tkq7f%2FLAPw%2Ba6m65ySGXCnwvY2fEXX5Bkf7obAtsA%2B%2B%2FoMMy0OxUrkUzPUZbFhGY%2FtaKFT2sNCVF7TefRFP4pW4rdA%2FNI1p%2FCxVN6y2VotXAmlAGPkSrT4yKseiEEq7SQhKjTkLzqQuWtbrlriIIrmO8QTMvM%2BY1sGMhLNqKDln8EkQ5IMHuKNxCEKiGU0wmaGoc52WBLboEwE32Y%2BTj4fvgL%2B9pR2S7tqx8gVazSWPnfOZKMaHnhJlkYdGd8i8ZnvMbDCS0CgWK5BWFc1dDiSaCXoSFXy3DseS2MByol1CCewaAW%2BPeNN%2Bt6X%2BVhXJUDS9yJnJazXGGlQKV47WU57jFJWRRcdhXgYPQJwHQIsOPq8OG8BdrUqmz0jYTwmPq1by%2Bw%2BN1br8M2Dyxwqe0NcVDGzyWQdf669MddKvSOrwt3vqoU2kLQ%3D%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ASIAS7RZNLNQ5MNH2PQG%2F20260508%2Feu-west-3%2Fs3%2Faws4_request&X-Amz-Date=20260508T063057Z&X-Amz-Expires=43200&X-Amz-SignedHeaders=host&X-Amz-Signature=9f5dfc4cdc605a5d3d1b174f7d74fd5efd62f7b958580fa8b20aeb3d468d9c64";
    var lidarPresignedUrls =
        List.of(
            "https://3dbag-dataset.s3.eu-west-3.amazonaws.com/lidar-data/61%20Rue%20de%20la%20Chapelle%20-%2075018%20Paris-lidar/LHD_FXX_0652_6867_PTS_LAMB93_IGN69.copc.laz?response-content-disposition=inline&X-Amz-Content-Sha256=UNSIGNED-PAYLOAD&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEP%2F%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCWV1LXdlc3QtMyJHMEUCIHTwEEY2arDpmw5S%2BDvk1A3IyRA1sX09H%2BqanUqJ3NBdAiEAtLeDcG3XaHrv43ZX5sRtAMsV1DOhFUYYo1cRBdFdkZsq1wMIyP%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FARAAGgwyMDUyMDUxMDE0MDkiDGdgaVYWc%2F%2FIH6ND9SqrA5IgJWIiTSSrYuID2IuKHZhFvVZDpn2Gxh2T1%2FN8zEapB5m7FURtuA3IhnR5MY1gW%2BySfHPlJ35IKWdG8SMnSjXl0Qrt6ajwWbkLrIjM20fzBpqp%2FMNvRTujBvX92fJjWZsgp1cQvGn4ko01G27HdIf7%2Bywxzme6QNRV4%2B9w464cO6L3Co0eT8WrdBizpwxgO5lv%2FfV4U7C1gBIuwWqctjdhWD3RzDd6hypklk6qeNcv4PSafc0VbQ2W%2BqUGzm4hJiNVu3lqTlZc0VDomEohojBhq6Pbztwyt9A%2B1%2BJOhjMKooO8g6x0mO8hRwNzfl1rY7wPYEClNWp8Ll34M5Cfy7sSc9eiYU5ejYD987jxgXF12%2BN%2BrOd%2FcBhMo01fynyrm7IZslKPYfuPIHWddwdrTuGLxKShKd6HN8lHvscXtIm7P899sAwW8zHlghG1OQThl%2FZ%2FXB8LUSRiSuZ1qSDP0DxtLlonolRXvbdKlSYCLMuuey0uUkhCtq4Pgy0O67UKIywEs4vN9W0hpLxaLza7DcesB%2F9LvQDN4HDHdztzcbaN%2FB9mgG2Sq7F0D4gwmYX2zwY63gJR%2BbQ1pAKSolxFL9cP1eQM2XxTVw0rczy25AQxhgT4SayGjjDiJUdlrNR7U52rSYMcYQUImvxIcXV9Uz37sP%2Bv2s%2FX1HxcOuq28a%2F87RysVy4L%2F%2BUgw1WanM%2FA%2FnaqWuIin7FVeR9f4NOzHdMMw%2Biiw4bHkUmJ4sxiOuW2FQBihTLWqFNojsQC16F3E0aZjfHWkc97raCSb2X5amXWjKdiVeSsLLPqLPUuf9ui1gCtroEej%2FB%2BMpQmvuWEqDKnn3rzvqDLRPoHAl6edzKGr1AhQ0Quoo%2BlW3dZ%2BclxxEDOPd4EW0I1tNo%2Biq%2BdMNh5cRNw1w7ycnd7qImiFf1ASd3Q4asyLyGyxoC%2BdcbhdR4E5xwfFAR64t%2FD%2BOhmXarlXUDZ40F38Isstcl0Ja9Hmr8852RN3yO9BYScE0qcIB1a6Wo7OIse0IGIC6ZfPLfZqH2JrCZcde3uHsuSPoeTrw%3D%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ASIAS7RZNLNQ34GFP6JO%2F20260508%2Feu-west-3%2Fs3%2Faws4_request&X-Amz-Date=20260508T063138Z&X-Amz-Expires=43200&X-Amz-SignedHeaders=host&X-Amz-Signature=ebe927a368e455456d338038a6dfc69e3917d721c327327221ba911d060738b2");

    var actual =
        subject.generateCityJson(
            CityJsonGenerationRequest.builder()
                .geoJsonBuildingPresignedUrl(geoJsonBuildingPresignedUrl)
                .lidarPresignedUrls(lidarPresignedUrls)
                .build(),
            null);

    assertNotNull(actual);
    assertNotNull(actual.getCityJsonUrl());
    assertNotNull(actual.getExpirationDateTime());
    log.info("url {}", actual.getCityJsonUrl());
    log.info("expiration {}", actual.getExpirationDateTime());
    // assertTrue(actual.getCityJsonUrl().contains(""));
  }

  private RestTemplate getRestTemplate() {
    var template = new RestTemplate();
    template.setInterceptors(Collections.singletonList(apiKeyInterceptor()));
    return template;
  }

  private ClientHttpRequestInterceptor apiKeyInterceptor() {
    return (request, body, execution) -> {
      String key = API_KEY;
      if (key != null && !key.isBlank()) {
        request.getHeaders().set("x-api-key", key);
      }
      return execution.execute(request, body);
    };
  }
}
