package app.bpartners.geojobs.service.area.mutation;

import app.bpartners.geojobs.service.area.mutation.model.MutationContext;
import app.bpartners.geojobs.service.area.mutation.model.MutationType;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MutationComputer {
  // The mutation-degat-api's ONNX model requires fixed 256x256 inputs.
  private static final int MUTATION_MODEL_INPUT_SIZE = 256;

  private final MutationApi api;
  private final ImageDownloader imageDownloader;
  private final ImageResizer imageResizer;

  public MutationType apply(MutationContext context) {
    var older = context.older();
    var mostRecent = context.mostRecent();
    log.info(
        "Computing mutation: olderYear={}, olderImageUrl={}, mostRecentYear={},"
            + " mostRecentImageUrl={}",
        older.year(),
        older.imagePresignedUrl().value(),
        mostRecent.year(),
        mostRecent.imagePresignedUrl().value());

    var oldImageFile = imageDownloader.download(older.imagePresignedUrl().value());
    var recentImageFile = imageDownloader.download(mostRecent.imagePresignedUrl().value());

    var resizedOld = imageResizer.resizePhoto(oldImageFile, MUTATION_MODEL_INPUT_SIZE);
    var resizedRecent = imageResizer.resizePhoto(recentImageFile, MUTATION_MODEL_INPUT_SIZE);
    var resizedMask = imageResizer.resizeMask(context.maskImageFile(), MUTATION_MODEL_INPUT_SIZE);

    var filename = "mutation_" + UUID.randomUUID();
    var mutationResponse = api.detectMutation(resizedOld, resizedRecent, resizedMask, filename);

    log.info(
        "Mutation computed: olderYear={}, mostRecentYear={}, mutation={}",
        older.year(),
        mostRecent.year(),
        mutationResponse.mutation());

    return mutationResponse.mutation();
  }
}
