package app.bpartners.geojobs.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class ZoneSimilarityConf {
  public static final String DEFAULT_MIN_IOU = "0.90";
  public static final String DEFAULT_MAX_DISPLACEMENT_IN_METERS = "2.0";

  private final boolean enabled;
  private final double minIou;
  private final double maxDisplacementInMeters;

  @Autowired
  public ZoneSimilarityConf(
      @Value("${detection.reuse.approximate.enabled:true}") boolean enabled,
      @Value("${detection.reuse.approximate.min-iou:" + DEFAULT_MIN_IOU + "}") double minIou,
      @Value(
              "${detection.reuse.approximate.max-displacement-in-meters:"
                  + DEFAULT_MAX_DISPLACEMENT_IN_METERS
                  + "}")
          double maxDisplacementInMeters) {
    this.enabled = enabled;
    this.minIou = minIou;
    this.maxDisplacementInMeters = maxDisplacementInMeters;
  }

  public static ZoneSimilarityConf defaults() {
    return new ZoneSimilarityConf(
        true,
        Double.parseDouble(DEFAULT_MIN_IOU),
        Double.parseDouble(DEFAULT_MAX_DISPLACEMENT_IN_METERS));
  }
}
