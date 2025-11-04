package app.bpartners.geojobs.endpoint.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ZoneTilingJobIT extends FacadeIT {

  @Autowired ObjectMapper om;

  @Test
  void can_be_unmarshalled() throws JsonProcessingException {
    var ztjAsString =
        "{\"id\":\"9ea47e0e-2484-4cc2-affe-f128c651adb0\",\"zoneName\":\"Lyon\",\"emailReceiver\":\"lou@bpartners.app\",\"submissionInstant\":\"2024-02-14T14:35:51.313892Z\",\"statusHistory\":[{\"id\":\"2523a6f0-8b95-4d1e-9d94-3e5118688f1b\",\"progression\":\"PENDING\",\"health\":\"UNKNOWN\",\"creationDatetime\":\"2024-02-14T14:35:51.433813Z\",\"message\":null,\"jobId\":\"9ea47e0e-2484-4cc2-affe-f128c651adb0\",\"jobType\":\"TILING\"}],\"pending\":true}";

    var ztj = om.readValue(ztjAsString, ZoneTilingJob.class);
    assertEquals("9ea47e0e-2484-4cc2-affe-f128c651adb0", ztj.getId());
  }
}
