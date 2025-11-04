package app.bpartners.geojobs.service.annotator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AnnotatorUserInfoGetter {
  private final Map<String, String> geoJobsUserInfo;

  public AnnotatorUserInfoGetter(
      ObjectMapper om, @Value("${annotator.geojobs.user.info}") String geoJobsUserInfoAsString)
      throws JsonProcessingException {
    this.geoJobsUserInfo = om.readValue(geoJobsUserInfoAsString, new TypeReference<>() {});
    ;
  }

  public String getUserId() {
    return geoJobsUserInfo.get("userId");
  }

  public String getTeamId() {
    return geoJobsUserInfo.get("teamId");
  }
}
