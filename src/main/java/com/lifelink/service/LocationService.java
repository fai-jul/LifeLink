package com.lifelink.service;

import org.json.JSONArray;
import org.json.JSONObject;

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
public class LocationService {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

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
            JSONArray results = new JSONArray(response.body());
            if (results.isEmpty()) return Optional.empty();
            JSONObject first = results.getJSONObject(0);
            return Optional.of(new Coordinates(first.getDouble("lat"), first.getDouble("lon")));
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