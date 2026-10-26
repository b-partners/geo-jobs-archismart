package app.bpartners.geojobs.service.coverage;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class CoverageConf {
  private final int hdMaxPrecisionInCm;

  public CoverageConf(@Value("${coverage.hd.max.precision.cm}") int hdMaxPrecisionInCm) {
    this.hdMaxPrecisionInCm = hdMaxPrecisionInCm;
  }
}
