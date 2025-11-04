package app.bpartners.geojobs.endpoint.rest.readme.monitor.factory;

import app.bpartners.geojobs.endpoint.rest.readme.monitor.ReadmeMonitorConf;
import app.bpartners.geojobs.endpoint.rest.readme.monitor.model.ReadmeRequestCreator;
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
