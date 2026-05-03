package com.travel.planner.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.planner.exception.InvalidInputException;
import com.travel.planner.model.enums.TransportationMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OrsService {

    private static final Logger log = LoggerFactory.getLogger(OrsService.class);
    private static final String BASE_URL = "https://api.openrouteservice.org";

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OrsService(@Value("${ors.api-key}") String apiKey, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
    }

    /**
     * 地址 → 經緯度，使用 Nominatim（OpenStreetMap），免費不需 API Key
     * 回傳 [latitude, longitude]，失敗時回傳 null
     */
    public double[] geocode(String address) {
        try {
            String encoded = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=" + encoded;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "TravelPlannerApp/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            if (!root.isArray() || root.isEmpty()) {
                log.warn("Geocoding 找不到地址: {}", address);
                return null;
            }

            double lat = root.get(0).path("lat").asDouble();
            double lng = root.get(0).path("lon").asDouble();
            return new double[]{lat, lng};

        } catch (Exception e) {
            log.error("Geocoding 失敗，地址: {}，原因: {}", address, e.getMessage());
            return null;
        }
    }

    /**
     * 多點路線計算（Directions），使用 ORS API
     * latLngPoints: [[lat, lng], [lat, lng], ...]
     */
    public OrsRouteResult getDirections(List<double[]> latLngPoints, TransportationMethod method) {
        try {
            String profile = toProfile(method);
            String url = BASE_URL + "/v2/directions/" + profile + "/geojson";

            // ORS 要求 [longitude, latitude] 順序
            List<double[]> orsCoords = latLngPoints.stream()
                    .map(p -> new double[]{p[1], p[0]})
                    .toList();

            String body = objectMapper.writeValueAsString(Map.of("coordinates", orsCoords));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json, application/geo+json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new InvalidInputException("ORS 路線計算失敗（HTTP " + response.statusCode() + "），請確認 API Key 是否正確");
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode feature = root.path("features").get(0);
            JsonNode summary = feature.path("properties").path("summary");
            JsonNode geomCoords = feature.path("geometry").path("coordinates");

            double distanceMeters = summary.path("distance").asDouble();
            double durationSeconds = summary.path("duration").asDouble();

            // 轉換回 [latitude, longitude] 供 Leaflet 使用
            List<double[]> geometry = new ArrayList<>();
            for (JsonNode coord : geomCoords) {
                geometry.add(new double[]{coord.get(1).asDouble(), coord.get(0).asDouble()});
            }

            return new OrsRouteResult(distanceMeters, durationSeconds, geometry);

        } catch (InvalidInputException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidInputException("路線計算失敗：" + e.getMessage());
        }
    }

    private String toProfile(TransportationMethod method) {
        return switch (method) {
            case WALKING -> "foot-walking";
            case PUBLIC_TRANSIT, TAXI, SELF_DRIVING -> "driving-car";
        };
    }
}
