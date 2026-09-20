package com.civicfix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DuplicateCheckDto {

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    private Double radiusMeters = 100.0; // Default radius: 100 meters

    public DuplicateCheckDto() {
    }

    public DuplicateCheckDto(String category, Double latitude, Double longitude, Double radiusMeters) {
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusMeters = radiusMeters != null ? radiusMeters : 100.0;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getRadiusMeters() {
        return radiusMeters;
    }

    public void setRadiusMeters(Double radiusMeters) {
        this.radiusMeters = radiusMeters;
    }
}
