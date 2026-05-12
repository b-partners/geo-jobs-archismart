package app.bpartners.geojobs.model.lidar.planes.postprocessing;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.model.lidar.planes.conf.Plane3DExtractorConf;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import app.bpartners.geojobs.model.lidar.planes.topology.RoofReconstructor;
import app.bpartners.geojobs.model.lidar.planes.topology.RoofRelationClassifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.locationtech.jts.geom.Polygon;

public class PlanesPostProcessingProcessor implements Function<Collection<Plane3D>, List<Plane3D>> {
  private final List<LasPointGeometry> points;

  private final ChimneyFixer chimneyFixer;
  private final Plane3DMerger closedPlane3DMerger;
  private final DelimitationFiller delimitationFiller;
  private final InvalidPlane3DFilter invalidPlane3DFilter;
  private final RoofBoundaryClipper roofBoundaryClipper;
  private final RoofReconstructor roofReconstructor;

  public PlanesPostProcessingProcessor(
      Polygon roofDelimitation,
      Plane3DExtractorConf conf,
      Collection<LasPointGeometry> points,
      Plane3DExtractionStepExporter exporter) {
    this.points = new ArrayList<>(points);
    this.chimneyFixer = new ChimneyFixer(conf.chimneyFixerConf().maxChimneyArea(), exporter);

    this.closedPlane3DMerger =
        new Plane3DMerger(
            conf.closedPlaneMergerConf().epsilonSlope(),
            conf.closedPlaneMergerConf().epsilonZDistance(),
            conf.closedPlaneMergerConf().epsilonXYDistance());
    this.delimitationFiller =
        new DelimitationFiller(
            conf.delimitationFillerConf().maxEmptyCell(),
            conf.delimitationFillerConf().minCellPointsSize(),
            conf.delimitationFillerConf().gridSize());
    this.invalidPlane3DFilter =
        new InvalidPlane3DFilter(conf.planeConf().min2DArea(), conf.planeConf().compactness());
    this.roofBoundaryClipper = new RoofBoundaryClipper(roofDelimitation);
    this.roofReconstructor =
        new RoofReconstructor(
            RoofRelationClassifier.RoofRelationClassifierConf.builder()
                .angleThresholdDeg(5)
                .build());
  }

  @Override
  public List<Plane3D> apply(Collection<Plane3D> planes) {
    var postProcessed = this.invalidPlane3DFilter.apply(planes);
    postProcessed = this.delimitationFiller.apply(postProcessed, points);
    postProcessed = this.chimneyFixer.apply(postProcessed);
    postProcessed =
        this.delimitationFiller.apply(this.closedPlane3DMerger.apply(postProcessed), points);
    postProcessed = this.invalidPlane3DFilter.apply(postProcessed);
    postProcessed = this.roofBoundaryClipper.apply(postProcessed);
    return this.roofReconstructor.apply(postProcessed);
  }
}
