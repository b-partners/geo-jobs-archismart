package app.bpartners.geojobs.endpoint.rest.validator;

import app.bpartners.geojobs.endpoint.rest.model.ImportZoneTilingJob;
import app.bpartners.geojobs.model.exception.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class ZoneTilingJobValidator {

  public void accept(ImportZoneTilingJob rest) {
    StringBuilder stringBuilder = new StringBuilder();
    if (rest.getCreateZoneTilingJob() == null) {
      stringBuilder.append("createZoneTilingJob is mandatory. ");
    }
    if (rest.getBucketName() == null) {
      stringBuilder.append("bucketName is mandatory. ");
    }
    String exceptionMsg = stringBuilder.toString();
    if (!exceptionMsg.isEmpty()) {
      throw new BadRequestException(exceptionMsg);
    }
  }
}
