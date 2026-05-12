package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import java.util.*;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class TileDuplicationRemover implements Function<List<Tile>, List<Tile>> {

  @Override
  public List<Tile> apply(List<Tile> tiles) {
    Map<TileCoordinates, Tile> map = new HashMap<>();
    for (Tile tile : tiles) {
      TileCoordinates key = tile.getCoordinates();
      Tile existing = map.get(key);

      if (existing == null || tile.getCreationDatetime().isAfter(existing.getCreationDatetime())) {
        map.put(key, tile);
      }
    }
    return map.values().stream().toList();
  }
}
