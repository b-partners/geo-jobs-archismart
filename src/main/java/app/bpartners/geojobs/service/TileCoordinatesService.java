package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class TileCoordinatesService {

  public List<TileCoordinates> completeQuadrilateral(List<TileCoordinates> tiles) {
    if (tiles == null || tiles.size() <= 2) {
      return tiles;
    }
    int zoom = tiles.getFirst().getZ();
    int minX = tiles.stream().mapToInt(TileCoordinates::getX).min().orElseThrow();
    int maxX = tiles.stream().mapToInt(TileCoordinates::getX).max().orElseThrow();
    int minY = tiles.stream().mapToInt(TileCoordinates::getY).min().orElseThrow();
    int maxY = tiles.stream().mapToInt(TileCoordinates::getY).max().orElseThrow();

    int cols = (maxX - minX) + 1;
    int rows = (maxY - minY) + 1;
    int expectedTileCount = cols * rows;
    if (tiles.size() == expectedTileCount) {
      return tiles;
    }
    Set<TileCoordinates> completed = new HashSet<>(tiles);
    for (int x = minX; x <= maxX; x++) {
      for (int y = minY; y <= maxY; y++) {
        completed.add(new TileCoordinates().x(x).y(y).z(zoom));
      }
    }
    return new ArrayList<>(completed);
  }

  public Integer colNumbers(List<TileCoordinates> tiles) {
    if (tiles.isEmpty()) {
      return 0;
    }
    int minX = tiles.stream().mapToInt(TileCoordinates::getX).min().orElseThrow();
    int maxX = tiles.stream().mapToInt(TileCoordinates::getX).max().orElseThrow();

    return maxX - minX + 1;
  }

  public Integer rowNumbers(List<TileCoordinates> tiles) {
    if (tiles.isEmpty()) {
      return 0;
    }
    int minY = tiles.stream().mapToInt(TileCoordinates::getY).min().orElseThrow();
    int maxY = tiles.stream().mapToInt(TileCoordinates::getY).max().orElseThrow();
    return maxY - minY + 1;
  }
}
