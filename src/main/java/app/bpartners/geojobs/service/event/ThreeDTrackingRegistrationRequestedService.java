package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.ThreeDTrackingRegistrationRequested;
import app.bpartners.geojobs.service.ThreeDTrackingRegister;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ThreeDTrackingRegistrationRequestedService
    implements Consumer<ThreeDTrackingRegistrationRequested> {
  private final ThreeDTrackingRegister threeDTrackingRegister;

  @Override
  public void accept(ThreeDTrackingRegistrationRequested event) {
    threeDTrackingRegister.accept(
        event.getRequestId(), event.getCommunityOwnerId(), event.getFailureId());
  }
}
