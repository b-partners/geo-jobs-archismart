package app.bpartners.geojobs.repository.model.cityjson;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "city_json_texture")
@Builder(toBuilder = true)
public class CityJSONTexture {
  @Id private String id;

  @Column(name = "image_uri")
  private String imageUri;

  @Column(name = "image_width")
  private int imageWidth;

  @Column(name = "image_height")
  private int imageHeight;

  @Column(name = "tile_x")
  private int tileX;

  @Column(name = "tile_y")
  private int tileY;

  @Column(name = "tile_image_size_px")
  private int tileImageSizePx;

  @Column(name = "zoom")
  private int zoom;

  @ManyToOne
  @JoinColumn(name = "city_json_request_id")
  private CityJSONRequest cityJsonRequest;
}
