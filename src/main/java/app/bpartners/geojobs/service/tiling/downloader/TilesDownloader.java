package app.bpartners.geojobs.service.tiling.downloader;

import app.bpartners.geojobs.repository.model.ParcelContent;
import java.io.File;
import java.util.function.Function;

public interface TilesDownloader extends Function<ParcelContent, File> {}
