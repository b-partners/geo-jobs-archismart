package app.bpartners.geojobs.service.detection.inference;

import static app.bpartners.geojobs.service.detection.inference.DeployedModelCard.ProblemType.CHANGE_DETECTION;
import static app.bpartners.geojobs.service.detection.inference.DeployedModelCard.ProblemType.CLASSIFICATION;
import static app.bpartners.geojobs.service.detection.inference.DeployedModelCard.ProblemType.OBJECT_DETECTION;
import static app.bpartners.geojobs.service.detection.inference.DeployedModelCard.ProblemType.SEMANTIC_SEGMENTATION;
import static app.bpartners.geojobs.service.detection.inference.InferenceApiClient.API_KEY_HEADER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class InferenceApiClientTest {
  private static final String URL = "https://inference.api";
  private static final String KEY = "secret";
  private static final String MODEL_CARDS_JSON =
      """
      [
        {"modelCardId":"damages-ignore-index-c4","problemType":"SEMANTIC_SEGMENTATION",
         "imagesRequired":1,"maskRequired":true,"imageWidth":1024,"imageHeight":1024,
         "runnable":true,"metrics":{"miou":0.5}},
        {"modelCardId":"cimetiere-tombe-v1.0","problemType":"OBJECT_DETECTION",
         "imagesRequired":1,"maskRequired":false},
        {"modelCardId":"bp-toiture-mutation-v1.0","problemType":"CHANGE_DETECTION",
         "imagesRequired":2,"maskRequired":true,"runnable":false},
        {"modelCardId":"signalisation-state-binary-dedup-raw","problemType":"CLASSIFICATION",
         "imagesRequired":1,"maskRequired":false,"objectsRequired":true,"runnable":true}
      ]
      """;
  private static final String INFERENCE_JSON =
      """
      {"testfn":{"fileref":"","base64_img_data":"","file_attributes":{},"size":"",
       "regions":{"0":{"region_attributes":{"label":"roof_ardoise"},
       "shape_attributes":{"name":"polygon","all_points_x":[425,424],"all_points_y":[764,765]}}}}}
      """;

  RestTemplate restTemplate = new RestTemplate();
  MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
  InferenceApiClient subject = new InferenceApiClient(restTemplate, URL, KEY);

  @Test
  void get_model_cards_ok() {
    server
        .expect(requestTo(URL + "/modelcards"))
        .andExpect(method(GET))
        .andExpect(header(API_KEY_HEADER, KEY))
        .andRespond(withSuccess(MODEL_CARDS_JSON, APPLICATION_JSON));

    var modelCards = subject.getModelCards();

    assertEquals(4, modelCards.size());
    var damages = modelCards.get(0);
    assertEquals("damages-ignore-index-c4", damages.modelCardId());
    assertEquals(SEMANTIC_SEGMENTATION, damages.problemType());
    assertTrue(damages.maskRequired());
    assertEquals(1024, damages.imageWidth());
    assertTrue(damages.isRunnable());
    assertEquals(OBJECT_DETECTION, modelCards.get(1).problemType());
    assertTrue(modelCards.get(1).isRunnable());
    assertEquals(CHANGE_DETECTION, modelCards.get(2).problemType());
    assertFalse(modelCards.get(2).isRunnable());
    assertEquals(CLASSIFICATION, modelCards.get(3).problemType());
    server.verify();
  }

  @Test
  void get_model_cards_throws_when_unauthorized() {
    server.expect(requestTo(URL + "/modelcards")).andRespond(withStatus(UNAUTHORIZED));

    assertThrows(IllegalStateException.class, () -> subject.getModelCards());
  }

  @Test
  void infer_ok_and_omits_absent_fields() {
    server
        .expect(requestTo(URL + "/inference"))
        .andExpect(method(POST))
        .andExpect(header(API_KEY_HEADER, KEY))
        .andExpect(jsonPath("$.modelCardId").value("damages-ignore-index-c4"))
        .andExpect(jsonPath("$.image1").value("aW1n"))
        .andExpect(jsonPath("$.mask1").value("bWFzaw=="))
        .andExpect(jsonPath("$.filename").value("tile.jpg"))
        .andExpect(jsonPath("$.zoomLevel").value(21))
        .andExpect(jsonPath("$.spatialResolution").value(5.0))
        .andExpect(jsonPath("$.image2").doesNotExist())
        .andExpect(jsonPath("$.mask2").doesNotExist())
        .andRespond(withSuccess(INFERENCE_JSON, APPLICATION_JSON));

    var response =
        subject.infer(
            InferenceRequest.builder()
                .modelCardId("damages-ignore-index-c4")
                .image1("aW1n")
                .mask1("bWFzaw==")
                .filename("tile.jpg")
                .zoomLevel(21)
                .spatialResolution(5.0)
                .build());

    var image = response.getImages().get("testfn");
    var region = image.getRegions().get("0");
    assertEquals("roof_ardoise", region.getRegionAttributes().get("label"));
    assertEquals("polygon", region.getShapeAttributes().getName());
    assertEquals(2, region.getShapeAttributes().getAllPointsX().size());
    server.verify();
  }

  @Test
  void infer_throws_when_bad_request() {
    server.expect(requestTo(URL + "/inference")).andRespond(withStatus(BAD_REQUEST));

    assertThrows(
        IllegalStateException.class,
        () -> subject.infer(InferenceRequest.builder().modelCardId("x").image1("aW1n").build()));
  }
}
