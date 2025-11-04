package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import java.io.File;
import java.util.List;
import org.apache.commons.lang3.function.TriFunction;

public interface TileObjectDetector
    extends TriFunction<
        TileDetectionTask, File, List<DetectableObjectConfiguration>, DetectionResponse> {}
