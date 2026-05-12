package app.bpartners.geojobs.service.cityjson.texture;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.LAMBERT_93;

import app.bpartners.geojobs.model.lidar.planes.algorithm.Vector3DUtils;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.cityjson.texture.model.CityJsonWithVertices;
import app.bpartners.geojobs.service.cityjson.texture.model.RasterInfo;
import app.bpartners.geojobs.service.cityjson.texture.model.RowCol;
import app.bpartners.geojobs.service.cityjson.texture.model.TextureInfo;
import app.bpartners.geojobs.service.cityjson.texture.model.TexturedCityJson;
import app.bpartners.geojobs.service.cityjson.texture.model.UV;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.math.Vector3D;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CityJsonTextureDomainService {

  public static final String VALUES_ATTRIBUTE_NAME = "values";
  public static final String DEFAULT_ATTRIBUTE_NAME = "default";
  public static final String TEXTURE_ATTRIBUTE_NAME = "texture";
  public static final String MATERIAL_ATTRIBUTE_NAME = "material";
  private final ObjectMapper objectMapper;
  private final GeometrySquareMeterArea geometrySquareMeterArea;

  public CityJsonWithVertices toCityJsonFile(ObjectNode json) {
    ArrayNode rawVertices = (ArrayNode) json.get("vertices");

    double scaleX = 1.0;
    double scaleY = 1.0;
    double scaleZ = 1.0;

    double translateX = 0.0;
    double translateY = 0.0;
    double translateZ = 0.0;

    if (json.has("transform")) {
      JsonNode transform = json.get("transform");

      JsonNode scale = transform.get("scale");
      JsonNode translate = transform.get("translate");

      scaleX = scale.get(0).asDouble();
      scaleY = scale.get(1).asDouble();
      scaleZ = scale.get(2).asDouble();

      translateX = translate.get(0).asDouble();
      translateY = translate.get(1).asDouble();
      translateZ = translate.get(2).asDouble();
    }

    CoordinateReferenceSystem crs = LAMBERT_93;

    List<Vector3D> vertices = new ArrayList<>();

    for (JsonNode rawVertex : rawVertices) {
      double x = rawVertex.get(0).asDouble() * scaleX + translateX;
      double y = rawVertex.get(1).asDouble() * scaleY + translateY;
      double z = rawVertex.get(2).asDouble() * scaleZ + translateZ;

      vertices.add(new Vector3D(x, y, z));
    }

    return new CityJsonWithVertices(json, vertices, crs);
  }

  public List<Vector3D> project(
      List<Vector3D> vectors,
      CoordinateReferenceSystem sourceCrs,
      CoordinateReferenceSystem targetCrs) {
    return vectors.stream()
        .map(
            v -> {
              Point origin = geometryFactory.createPoint(new Coordinate(v.getX(), v.getY()));
              Point projected =
                  (Point) geometrySquareMeterArea.project(origin, sourceCrs, targetCrs);
              return new Vector3D(
                  projected.getCoordinate().x, projected.getCoordinate().y, v.getZ());
            })
        .toList();
  }

  public TexturedCityJson texture(
      CityJsonWithVertices cityJsonWithVertices, TextureInfo textureInfo) {
    ObjectNode cityJson = cityJsonWithVertices.json().deepCopy();

    RasterInfo rasterInfo = textureInfo.rasterInfo();

    List<Vector3D> vertices = cityJsonWithVertices.vertices();
    ObjectNode appearance = initAppearance(textureInfo);

    List<UV> vertexTexture = new ArrayList<>();
    Map<String, Integer> vertexTextureMap = new HashMap<>();

    ObjectNode cityObjects = (ObjectNode) cityJson.get("CityObjects");
    Iterator<JsonNode> objects = cityObjects.elements();

    while (objects.hasNext()) {
      ObjectNode cityObject = (ObjectNode) objects.next();

      if (!"Building".equals(cityObject.get("type").asText())) {
        continue;
      }

      ArrayNode geometries = (ArrayNode) cityObject.get("geometry");

      for (JsonNode geometryNode : geometries) {
        texturizeGeometry(
            (ObjectNode) geometryNode, vertices, rasterInfo, vertexTexture, vertexTextureMap);
      }
    }

    ArrayNode verticesTextureNode = objectMapper.createArrayNode();

    for (UV uv : vertexTexture) {
      ArrayNode uvNode = objectMapper.createArrayNode();
      uvNode.add(uv.u());
      uvNode.add(uv.v());
      verticesTextureNode.add(uvNode);
    }

    appearance.set("vertices-texture", verticesTextureNode);
    cityJson.set("appearance", appearance);

    return new TexturedCityJson(cityJson);
  }

  private ObjectNode initAppearance(TextureInfo textureInfo) {
    String imageDataUri = textureInfo.dataUri();

    String textureDataUri = null;
    if (imageDataUri != null) {
      textureDataUri = imageDataUri;
    }

    return initAppearance(textureDataUri);
  }

  private void texturizeGeometry(
      ObjectNode geometryNode,
      List<Vector3D> vertices,
      RasterInfo rasterInfo,
      List<UV> vertexTexture,
      Map<String, Integer> vertexTextureMap) {
    ObjectNode geometry = geometryNode;

    ObjectNode geometryAppearance = initGeometryAppearance();
    geometry.set("appearance", geometryAppearance);

    List<JsonNode> faces = extractFaces(geometry);

    ArrayNode surfaces = getSemanticSurfaces(geometry);
    boolean hasSemantics = surfaces != null && surfaces.size() == faces.size();

    List<String> surfaceTypes = resolveSurfaceTypes(surfaces, faces.size(), hasSemantics);

    for (int i = 0; i < faces.size(); i++) {
      processFace(
          faces.get(i),
          surfaceTypes.get(i),
          hasSemantics,
          vertices,
          rasterInfo,
          vertexTexture,
          vertexTextureMap,
          geometryAppearance);
    }
  }

  public List<UV> computeUv(List<Vector3D> coords, RasterInfo rasterInfo) {
    List<UV> result = new ArrayList<>();

    for (Vector3D coord : coords) {
      RowCol rowCol = rowColAffine(rasterInfo, coord.getX(), coord.getY());

      double u = rowCol.col() / rasterInfo.width();
      double v = 1.0 - (rowCol.row() / rasterInfo.height());

      result.add(new UV(u, v));
    }

    return result;
  }

  public List<Integer> deduplicateUvs(
      List<UV> uv, List<UV> vertexTexture, Map<String, Integer> vertexTextureMap) {
    List<Integer> vtIndices = new ArrayList<>();

    for (UV coordinate : uv) {
      String key = "%.6f,%.6f".formatted(coordinate.u(), coordinate.v());

      Integer existingIndex = vertexTextureMap.get(key);

      if (existingIndex == null) {
        int newIndex = vertexTexture.size();
        vertexTextureMap.put(key, newIndex);
        vertexTexture.add(coordinate);
        vtIndices.add(newIndex);
      } else {
        vtIndices.add(existingIndex);
      }
    }

    return vtIndices;
  }

  public ObjectNode initAppearance(String textureDataUri) {
    ObjectNode appearance = objectMapper.createObjectNode();

    ArrayNode textures = objectMapper.createArrayNode();
    ObjectNode texture = objectMapper.createObjectNode();
    texture.put("type", "PNG");
    texture.put("image", textureDataUri);
    textures.add(texture);

    ArrayNode materials = objectMapper.createArrayNode();

    ObjectNode roofMaterial = objectMapper.createObjectNode();
    roofMaterial.put("name", "roof_material");
    roofMaterial.put("ambientIntensity", 1.0);
    materials.add(roofMaterial);

    ObjectNode wallMaterial = objectMapper.createObjectNode();
    wallMaterial.put("name", "wall_gray");

    ArrayNode diffuseColor = objectMapper.createArrayNode();
    diffuseColor.add(0.6);
    diffuseColor.add(0.6);
    diffuseColor.add(0.6);

    wallMaterial.set("diffuseColor", diffuseColor);
    materials.add(wallMaterial);

    appearance.set("textures", textures);
    appearance.set("materials", materials);
    appearance.set("vertices-texture", objectMapper.createArrayNode());

    return appearance;
  }

  public ObjectNode initGeometryAppearance() {
    ObjectNode geometryAppearance = objectMapper.createObjectNode();

    ObjectNode texture = objectMapper.createObjectNode();
    ObjectNode textureDefault = objectMapper.createObjectNode();
    textureDefault.set(VALUES_ATTRIBUTE_NAME, objectMapper.createArrayNode());
    texture.set(DEFAULT_ATTRIBUTE_NAME, textureDefault);

    ObjectNode material = objectMapper.createObjectNode();
    ObjectNode materialDefault = objectMapper.createObjectNode();
    materialDefault.set(VALUES_ATTRIBUTE_NAME, objectMapper.createArrayNode());
    material.set(DEFAULT_ATTRIBUTE_NAME, materialDefault);

    geometryAppearance.set(TEXTURE_ATTRIBUTE_NAME, texture);
    geometryAppearance.set(MATERIAL_ATTRIBUTE_NAME, material);

    return geometryAppearance;
  }

  public void processFace(
      JsonNode face,
      String faceType,
      boolean hasSemantics,
      List<Vector3D> vertices,
      RasterInfo rasterInfo,
      List<UV> vertexTexture,
      Map<String, Integer> vertexTextureMap,
      ObjectNode geometryAppearance) {
    List<Vector3D> coords = getFaceCoords(face, vertices);

    if (coords.size() < 3) {
      return;
    }

    List<UV> uv = computeUv(coords, rasterInfo);

    List<Integer> vtIndices = deduplicateUvs(uv, vertexTexture, vertexTextureMap);

    boolean roof = hasSemantics ? isRoofSemantic(faceType) : isRoof(coords);

    ArrayNode textureValues =
        (ArrayNode)
            geometryAppearance
                .get(TEXTURE_ATTRIBUTE_NAME)
                .get(DEFAULT_ATTRIBUTE_NAME)
                .get(VALUES_ATTRIBUTE_NAME);

    ArrayNode materialValues =
        (ArrayNode)
            geometryAppearance
                .get(MATERIAL_ATTRIBUTE_NAME)
                .get(DEFAULT_ATTRIBUTE_NAME)
                .get(VALUES_ATTRIBUTE_NAME);

    if (roof) {
      ArrayNode ringTextureIndices = objectMapper.createArrayNode();

      for (Integer index : vtIndices) {
        ringTextureIndices.add(index);
      }

      ArrayNode faceTextureIndices = objectMapper.createArrayNode();
      faceTextureIndices.add(ringTextureIndices);

      textureValues.add(faceTextureIndices);
      materialValues.add(0);
    } else {
      textureValues.addNull();

      if (!hasSemantics) {
        materialValues.add(1);
      }
    }
  }

  public List<JsonNode> extractFaces(ObjectNode geometry) {
    String type = geometry.get("type").asText();
    ArrayNode boundaries = (ArrayNode) geometry.get("boundaries");

    List<JsonNode> faces = new ArrayList<>();

    if ("Solid".equals(type)) {
      for (JsonNode shell : boundaries) {
        for (JsonNode face : shell) {
          faces.add(face);
        }
      }
    } else {
      for (JsonNode face : boundaries) {
        faces.add(face);
      }
    }

    return faces;
  }

  public List<Vector3D> getFaceCoords(JsonNode face, List<Vector3D> vertices) {
    JsonNode outerRing = face.get(0);

    List<Vector3D> coords = new ArrayList<>();

    for (JsonNode vertexIndexNode : outerRing) {
      int vertexIndex = vertexIndexNode.asInt();
      coords.add(vertices.get(vertexIndex));
    }

    return coords;
  }

  public boolean isRoof(List<Vector3D> coords) {
    if (coords.size() < 3) {
      return false;
    }

    Vector3D p0 = coords.get(0);
    Vector3D p1 = coords.get(1);
    Vector3D p2 = coords.get(2);

    Vector3D v1 = new Vector3D(p1.getX() - p0.getX(), p1.getY() - p0.getY(), p1.getZ() - p0.getZ());
    Vector3D v2 = new Vector3D(p2.getX() - p0.getX(), p2.getY() - p0.getY(), p2.getZ() - p0.getZ());
    Vector3D normal = Vector3DUtils.cross(v1, v2);

    double unitZ = normal.getZ() / (normal.length() + 1e-12);

    return Math.abs(unitZ) > 0.7;
  }

  public boolean isRoofSemantic(String faceType) {
    return faceType != null && faceType.toLowerCase(Locale.ROOT).contains("roof");
  }

  public ArrayNode getSemanticSurfaces(ObjectNode geometry) {
    if (!geometry.has("semantics")) {
      return null;
    }

    JsonNode semantics = geometry.get("semantics");

    if (!semantics.has("surfaces")) {
      return null;
    }

    return (ArrayNode) semantics.get("surfaces");
  }

  public List<String> resolveSurfaceTypes(ArrayNode surfaces, int faceCount, boolean hasSemantics) {
    List<String> surfaceTypes = new ArrayList<>();

    if (hasSemantics) {
      for (JsonNode surface : surfaces) {
        String type = surface.has("type") ? surface.get("type").asText() : "WallSurface";

        surfaceTypes.add(type);
      }
    } else {
      for (int i = 0; i < faceCount; i++) {
        surfaceTypes.add(null);
      }
    }

    return surfaceTypes;
  }

  public RowCol rowColAffine(RasterInfo t, double x, double y) {
    double a = t.pixelWidth();
    double b = t.shearX();
    double c = t.originX();

    double d = t.shearY();
    double e = t.pixelHeight();
    double f = t.originY();

    double determinant = a * e - b * d;

    if (Math.abs(determinant) < 1e-12) {
      throw new IllegalArgumentException("Raster transform is not invertible");
    }

    double dx = x - c;
    double dy = y - f;

    double col = (e * dx - b * dy) / determinant;
    double row = (-d * dx + a * dy) / determinant;

    return new RowCol(row, col);
  }
}
