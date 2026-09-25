package com.lifelink.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/** Resolves human-readable locations without making location data mandatory. */
public class LocationService implements LocationProvider {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Optional<Coordinates> geocode(String location) {
        if (location == null || location.isBlank()) return Optional.empty();
        String query = URLEncoder.encode(location.trim(), StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=" + query))
                .timeout(Duration.ofSeconds(6))
                .header("User-Agent", "LifeLink/1.0 (desktop blood donation app)")
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return Optional.empty();
            JsonNode results = objectMapper.readTree(response.body());
            if (!results.isArray() || results.isEmpty()) return Optional.empty();
            JsonNode first = results.get(0);
            JsonNode latitude = first.get("lat");
            JsonNode longitude = first.get("lon");
            if (latitude == null || longitude == null) return Optional.empty();
            return Optional.of(new Coordinates(latitude.asDouble(), longitude.asDouble()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    public record Coordinates(double latitude, double longitude) {
    }
}