package app.bpartners.geojobs.service.cityjson.texture;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.LAMBERT_93;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.WGS84;
import static app.bpartners.geojobs.service.cityjson.texture.Converter.lonLatToPixelInTile;

import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.cityjson.texture.model.CityJsonWithVertices;
import app.bpartners.geojobs.service.cityjson.texture.model.RasterInfo;
import app.bpartners.geojobs.service.cityjson.texture.model.TextureInfo;
import app.bpartners.geojobs.service.cityjson.texture.model.TexturedCityJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.stereotype.Component;

// TODO: refactor
@Component
@RequiredArgsConstructor
public class CityJsonTextureDomainService {
  private static final String VALUES_ATTRIBUTE_NAME = "values";
  private static final String DEFAULT_ATTRIBUTE_NAME = "default";
  private static final String TEXTURE_ATTRIBUTE_NAME = "texture";
  private static final String MATERIAL_ATTRIBUTE_NAME = "material";

  private final ObjectMapper objectMapper;
  private final GeometrySquareMeterArea projector;

  public CityJsonWithVertices toCityJsonWithVertices(ObjectNode json) {
    ArrayNode rawVertices = (ArrayNode) json.get("vertices");

    double scaleX = 1.0;
    double scaleY = 1.0;
    double scaleZ = 1.0;

    double translateX = 0.0;
    double translateY = 0.0;
    double translateZ = 0.0;

    if (json.has("transform")) {
      var transform = json.get("transform");
      var scale = transform.get("scale");
      var translate = transform.get("translate");

      scaleX = scale.get(0).asDouble();
      scaleY = scale.get(1).asDouble();
      scaleZ = scale.get(2).asDouble();

      translateX = translate.get(0).asDouble();
      translateY = translate.get(1).asDouble();
      translateZ = translate.get(2).asDouble();
    }

    List<Coordinate> vertices = new ArrayList<>();
    for (var rawVertex : rawVertices) {
      double x = rawVertex.get(0).asDouble() * scaleX + translateX;
      double y = rawVertex.get(1).asDouble() * scaleY + translateY;
      double z = rawVertex.get(2).asDouble() * scaleZ + translateZ;
      vertices.add(new Coordinate(x, y, z));
    }

    return new CityJsonWithVertices(json, vertices, LAMBERT_93);
  }

  public TexturedCityJson texture(
      CityJsonWithVertices cityJsonWithVertices, TextureInfo textureInfo) {
    var rasterInfo = textureInfo.rasterInfo();
    var vertices = cityJsonWithVertices.vertices();
    var cityJson = cityJsonWithVertices.json().deepCopy();
    var cityObjects = (ObjectNode) cityJson.get("CityObjects");
    var objects = cityObjects.elements();

    List<Coordinate> vertexTexture = new ArrayList<>();
    ObjectNode appearance = initAppearance(textureInfo);
    Map<String, Integer> vertexTextureMap = new HashMap<>();
    while (objects.hasNext()) {
      var cityObject = (ObjectNode) objects.next();
      var supportedObjectTypes = List.of("Building", "BuildingPart");
      var cityObjectType = cityObject.get("type").asText();
      if (!supportedObjectTypes.contains(cityObjectType)) {
        continue;
      }

      var geometries = (ArrayNode) cityObject.get("geometry");
      for (var geometryNode : geometries) {
        texturizeGeometry(
            (ObjectNode) geometryNode, vertices, rasterInfo, vertexTexture, vertexTextureMap);
      }
    }

    var verticesTextureNode = objectMapper.createArrayNode();
    for (var uv : vertexTexture) {
      var uvNode = objectMapper.createArrayNode();
      uvNode.add(uv.getX());
      uvNode.add(uv.getY());
      verticesTextureNode.add(uvNode);
    }

    appearance.set("vertices-texture", verticesTextureNode);
    cityJson.set("appearance", appearance);
    return new TexturedCityJson(cityJson);
  }

  private ObjectNode initAppearance(TextureInfo textureInfo) {
    var imageDataUri = textureInfo.dataUri();
    String textureDataUri = null;
    if (imageDataUri != null) {
      textureDataUri = imageDataUri;
    }
    return initAppearance(textureDataUri);
  }

  private void texturizeGeometry(
      ObjectNode geometry,
      List<Coordinate> vertices,
      RasterInfo rasterInfo,
      List<Coordinate> vertexTexture,
      Map<String, Integer> vertexTextureMap) {
    var geometryAppearance = initGeometryAppearance();

    geometry.set("appearance", geometryAppearance);

    var faces = extractFaces(geometry);
    var semantics = getSemanticSurfaces(geometry);
    var surfaceTypes = resolveSurfaceTypes(semantics, faces.size());
    for (int i = 0; i < faces.size(); i++) {
      processFace(
          faces.get(i),
          surfaceTypes.get(i),
          vertices,
          rasterInfo,
          vertexTexture,
          vertexTextureMap,
          geometryAppearance);
    }
  }

  public List<Coordinate> getUV(List<Coordinate> vertices, RasterInfo info) {
    var result = new ArrayList<Coordinate>();

    for (var vertex : vertices) {
      var pixelVertex = getPixelCoordinate(info, vertex);
      double u = pixelVertex.getX() / info.width();
      double v = 1.0 - (pixelVertex.getY() / info.height());
      result.add(new Coordinate(u, v));
    }

    return result;
  }

  public List<Integer> deduplicateUvs(
      List<Coordinate> uv, List<Coordinate> vertexTexture, Map<String, Integer> vertexTextureMap) {
    List<Integer> vtIndices = new ArrayList<>();

    for (var coordinate : uv) {
      var key = "%.6f,%.6f".formatted(coordinate.getX(), coordinate.getY());
      var existingIndex = vertexTextureMap.get(key);

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
    var appearance = objectMapper.createObjectNode();
    var textures = objectMapper.createArrayNode();
    var texture = objectMapper.createObjectNode();

    texture.put("type", "PNG");
    texture.put("image", textureDataUri);
    textures.add(texture);

    var materials = objectMapper.createArrayNode();
    var roofMaterial = objectMapper.createObjectNode();

    roofMaterial.put("name", "roof_material");
    roofMaterial.put("ambientIntensity", 1.0);
    materials.add(roofMaterial);

    var wallMaterial = objectMapper.createObjectNode();
    wallMaterial.put("name", "wall_gray");

    var diffuseColor = objectMapper.createArrayNode();
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
    var geometryAppearance = objectMapper.createObjectNode();

    var texture = objectMapper.createObjectNode();
    var textureDefault = objectMapper.createObjectNode();
    textureDefault.set(VALUES_ATTRIBUTE_NAME, objectMapper.createArrayNode());
    texture.set(DEFAULT_ATTRIBUTE_NAME, textureDefault);

    var material = objectMapper.createObjectNode();
    var materialDefault = objectMapper.createObjectNode();
    materialDefault.set(VALUES_ATTRIBUTE_NAME, objectMapper.createArrayNode());
    material.set(DEFAULT_ATTRIBUTE_NAME, materialDefault);

    geometryAppearance.set(TEXTURE_ATTRIBUTE_NAME, texture);
    geometryAppearance.set(MATERIAL_ATTRIBUTE_NAME, material);

    return geometryAppearance;
  }

  public void processFace(
      JsonNode face,
      String faceType,
      List<Coordinate> vertices,
      RasterInfo rasterInfo,
      List<Coordinate> vertexTexture,
      Map<String, Integer> vertexTextureMap,
      ObjectNode geometryAppearance) {
    var coordinates = getFaceCoordinates(face, vertices);
    if (coordinates.size() < 3) {
      return;
    }

    var roof = isRoofSemantic(faceType);
    var uv = getUV(coordinates, rasterInfo);
    var vtIndices = deduplicateUvs(uv, vertexTexture, vertexTextureMap);

    var textureValues =
        (ArrayNode)
            geometryAppearance
                .get(TEXTURE_ATTRIBUTE_NAME)
                .get(DEFAULT_ATTRIBUTE_NAME)
                .get(VALUES_ATTRIBUTE_NAME);

    var materialValues =
        (ArrayNode)
            geometryAppearance
                .get(MATERIAL_ATTRIBUTE_NAME)
                .get(DEFAULT_ATTRIBUTE_NAME)
                .get(VALUES_ATTRIBUTE_NAME);

    if (!roof) {
      textureValues.addNull();
      materialValues.add(1);
      return;
    }

    var ringTextureIndices = objectMapper.createArrayNode();
    for (var index : vtIndices) {
      ringTextureIndices.add(index);
    }

    var faceTextureIndices = objectMapper.createArrayNode();
    faceTextureIndices.add(ringTextureIndices);

    textureValues.add(faceTextureIndices);
    materialValues.add(0);
  }

  public List<JsonNode> extractFaces(ObjectNode geometry) {
    var type = geometry.get("type").asText();
    var boundaries = (ArrayNode) geometry.get("boundaries");

    List<JsonNode> faces = new ArrayList<>();

    if ("Solid".equals(type)) {
      for (JsonNode shell : boundaries) {
        for (JsonNode face : shell) {
          faces.add(face);
        }
      }
    } else {
      for (var face : boundaries) {
        faces.add(face);
      }
    }

    return faces;
  }

  public List<Coordinate> getFaceCoordinates(JsonNode face, List<Coordinate> vertices) {
    var outerRing = face.get(0);
    List<Coordinate> coordinates = new ArrayList<>();
    for (var vertexIndexNode : outerRing) {
      int vertexIndex = vertexIndexNode.asInt();
      coordinates.add(vertices.get(vertexIndex));
    }

    return coordinates;
  }

  public boolean isRoofSemantic(String faceType) {
    return "RoofSurface".equals(faceType);
  }

  public List<String> getSemanticSurfaces(ObjectNode geometry) {
    if (!geometry.has("semantics")) {
      return null;
    }

    var semantics = geometry.get("semantics");
    if (!semantics.has("surfaces") || !semantics.has("values")) {
      return null;
    }

    var surfaces = (ArrayNode) semantics.get("surfaces");
    var values = (ArrayNode) semantics.get("values");
    if (values.get(0) instanceof ArrayNode) {
      values = (ArrayNode) values.get(0);
    }

    var result = new ArrayList<String>();
    for (var value : values) {
      var surfacesIndex = value.asInt();
      result.add(surfaces.get(surfacesIndex).get("type").asText());
    }
    return result;
  }

  public List<String> resolveSurfaceTypes(List<String> semantics, int faceCount) {
    List<String> surfaceTypes = new ArrayList<>();

    for (int i = 0; i < faceCount; i++) {
      if (semantics != null && i < semantics.size()) {
        var semantic = semantics.get(i);
        surfaceTypes.add(semantic);
      } else {
        surfaceTypes.add(null);
      }
    }

    return surfaceTypes;
  }

  public Coordinate getPixelCoordinate(RasterInfo info, Coordinate vertex) {
    var coordinate = new Coordinate(vertex.getX(), vertex.getY(), vertex.getZ());
    var vertexAsPoint = geometryFactory.createPoint(coordinate);
    var latLon = projector.project(vertexAsPoint, LAMBERT_93, WGS84);
    return lonLatToPixelInTile(
        latLon.getCoordinate(), info.tileX(), info.tileY(), info.zoom(), info.tileImageSizePx());
  }
}
