package app.bpartners.geojobs.validator;

import static app.bpartners.geojobs.endpoint.rest.model.DelimitationObjectType.BUILDING_ROOF;
import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.PARCEL_FREE_DELIMITATION;

import app.bpartners.geojobs.endpoint.rest.model.ThreeDAddressesRequest;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class ThreeDAddressesRequestValidator implements Consumer<ThreeDAddressesRequest> {
  @Override
  public void accept(ThreeDAddressesRequest threeDRequest) {
    if (threeDRequest == null
        || threeDRequest.getAddresses() == null
        || threeDRequest.getAddresses().isEmpty()
        || threeDRequest.getAddresses().stream()
            .anyMatch(
                address ->
                    address.getFullText() == null || address.getFullText().equalsIgnoreCase(""))) {
      throw new BadRequestException("Addresses can not be null or empty");
    }
    if (threeDRequest.getDelimitationType() != null
        && !PARCEL_FREE_DELIMITATION.equals(threeDRequest.getDelimitationType())) {
      throw new NotImplementedException(
          "Only PARCEL_FREE_DELIMITATION is supported to request 3D model on addresses for now but"
              + " actual is "
              + threeDRequest.getDelimitationType());
    }
    if (threeDRequest.getDelimitationObjectType() != null
        && !BUILDING_ROOF.equals(threeDRequest.getDelimitationObjectType())) {
      throw new NotImplementedException(
          "Only BUILDING_ROOF is supported to request 3D model on addresses for now but"
              + " actual is "
              + threeDRequest.getDelimitationObjectType());
    }
  }
}
