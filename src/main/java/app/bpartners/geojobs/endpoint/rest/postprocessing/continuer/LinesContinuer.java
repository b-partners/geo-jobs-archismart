package app.bpartners.geojobs.endpoint.rest.postprocessing.continuer;

import java.util.Set;
import java.util.function.Function;

public abstract sealed class LinesContinuer<T> implements Function<Set<T>, Set<T>>
    permits LatLonLinesContinuer, ParallelTiledLinesContinuer, TiledLinesContinuer {}
