package app.bpartners.geojobs.service.gouv.fr.rnb.component;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BuildingAddress(
    String id,
    String source,
    @JsonProperty("street_number") String streetNumber,
    @JsonProperty("street_rep") String streetRep,
    @JsonProperty("street") String street,
    @JsonProperty("city_name") String cityName,
    @JsonProperty("city_zipcode") String cityZipCode,
    @JsonProperty("city_insee_code") Integer cityInseeCode) {}
