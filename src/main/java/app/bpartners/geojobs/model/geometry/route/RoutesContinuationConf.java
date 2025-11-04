package app.bpartners.geojobs.model.geometry.route;

import app.bpartners.geojobs.model.geometry.quadrilateral.model.AlphaConf;

public record RoutesContinuationConf(
    AlphaConf alphaConf,
    UnionConf unionConf,
    ContinuationConf continuationConf,
    PrettyConf prettyConf) {}
