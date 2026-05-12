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

  @Column(name = "top_left_longitude")
  private double topLeftLongitude;

  @Column(name = "top_left_latitude")
  private double topLeftLatitude;

  @Column(name = "pixel_width")
  private double pixelWidth;

  @Column(name = "pixel_height")
  private double pixelHeight;

  @Column(name = "shear_x")
  private double shearX;

  @Column(name = "shear_y")
  private double shearY;

  @Column(name = "image_width")
  private int imageWidth;

  @Column(name = "image_height")
  private int imageHeight;

  @ManyToOne
  @JoinColumn(name = "city_json_request_id")
  private CityJSONRequest cityJsonRequest;
}
