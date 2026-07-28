package app.bpartners.geojobs.readme.monitor.factory;

import app.bpartners.geojobs.readme.monitor.ReadmeMonitorConf;
import app.bpartners.geojobs.readme.monitor.model.ReadmeRequestCreator;
import org.springframework.stereotype.Component;

@Component
public class ReadmeRequestCreatorFactory {
  public ReadmeRequestCreator createReadmeRequestCreator(ReadmeMonitorConf readmeMonitorConf) {
    return ReadmeRequestCreator.builder()
        .version(readmeMonitorConf.getVersion())
        .name(readmeMonitorConf.getName())
        .build();
  }
}
