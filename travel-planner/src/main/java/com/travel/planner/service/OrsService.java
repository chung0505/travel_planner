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
import java.util.StringJoiner;

@Service
public class OrsService {

    private static final Logger log = LoggerFactory.getLogger(OrsService.class);
    private static final String GEOCODE_URL = "https://maps.googleapis.com/maps/api/geocode/json";
    private static final String DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/json";

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OrsService(@Value("${google.maps.api-key}") String apiKey, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
    }

    /**
     * 地址 → 經緯度，使用 Google Geocoding API
     * 回傳 [latitude, longitude]，失敗時回傳 null
     */
    public double[] geocode(String address) {
        try {
            String encoded = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String url = GEOCODE_URL + "?address=" + encoded + "&key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            String status = root.path("status").asText();
            if (!"OK".equals(status)) {
                log.warn("Geocoding 找不到地址: {} (status={})", address, status);
                return null;
            }

            JsonNode location = root.path("results").get(0).path("geometry").path("location");
            double lat = location.path("lat").asDouble();
            double lng = location.path("lng").asDouble();
            return new double[]{lat, lng};

        } catch (Exception e) {
            log.error("Geocoding 失敗，地址: {}，原因: {}", address, e.getMessage());
            return null;
        }
    }

    /**
     * 多點路線計算，使用 Google Directions API
     * latLngPoints: [[lat, lng], [lat, lng], ...]
     */
    public OrsRouteResult getDirections(List<double[]> latLngPoints, TransportationMethod method) {
        if (method == TransportationMethod.PUBLIC_TRANSIT) {
            // Google Directions API 大眾運輸模式不支援 waypoints，逐段呼叫再合併
            return getTransitDirections(latLngPoints);
        }
        return getDrivingOrWalkingDirections(latLngPoints, method);
    }

    private OrsRouteResult getDrivingOrWalkingDirections(List<double[]> points, TransportationMethod method) {
        String origin = toLatLng(points.get(0));
        String destination = toLatLng(points.get(points.size() - 1));
        String mode = toMode(method);

        StringBuilder url = new StringBuilder(DIRECTIONS_URL)
                .append("?origin=").append(origin)
                .append("&destination=").append(destination)
                .append("&mode=").append(mode)
                .append("&key=").append(apiKey);

        if (points.size() > 2) {
            StringJoiner waypoints = new StringJoiner("|");
            for (int i = 1; i < points.size() - 1; i++) {
                waypoints.add(toLatLng(points.get(i)));
            }
            url.append("&waypoints=").append(waypoints);
        }

        return callDirectionsApi(url.toString());
    }

    private OrsRouteResult getTransitDirections(List<double[]> points) {
        double totalDistance = 0;
        double totalDuration = 0;
        List<double[]> geometry = new ArrayList<>();

        for (int i = 0; i < points.size() - 1; i++) {
            String url = DIRECTIONS_URL
                    + "?origin=" + toLatLng(points.get(i))
                    + "&destination=" + toLatLng(points.get(i + 1))
                    + "&mode=transit"
                    + "&key=" + apiKey;

            OrsRouteResult segment = callDirectionsApi(url);
            totalDistance += segment.distanceMeters();
            totalDuration += segment.durationSeconds();
            geometry.addAll(segment.geometry());
        }

        return new OrsRouteResult(totalDistance, totalDuration, geometry);
    }

    private OrsRouteResult callDirectionsApi(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            String status = root.path("status").asText();
            if (!"OK".equals(status)) {
                throw new InvalidInputException("路線計算失敗（" + status + "），請確認景點地址是否正確");
            }

            JsonNode route = root.path("routes").get(0);

            double totalDistance = 0;
            double totalDuration = 0;
            for (JsonNode leg : route.path("legs")) {
                totalDistance += leg.path("distance").path("value").asDouble();
                totalDuration += leg.path("duration").path("value").asDouble();
            }

            String encodedPolyline = route.path("overview_polyline").path("points").asText();
            List<double[]> geometry = decodePolyline(encodedPolyline);

            return new OrsRouteResult(totalDistance, totalDuration, geometry);

        } catch (InvalidInputException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidInputException("路線計算失敗：" + e.getMessage());
        }
    }

    /**
     * 解碼 Google Encoded Polyline Algorithm Format
     */
    private List<double[]> decodePolyline(String encoded) {
        List<double[]> result = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, val = 0;
            do {
                b = encoded.charAt(index++) - 63;
                val |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            lat += (val & 1) != 0 ? ~(val >> 1) : (val >> 1);

            shift = 0;
            val = 0;
            do {
                b = encoded.charAt(index++) - 63;
                val |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            lng += (val & 1) != 0 ? ~(val >> 1) : (val >> 1);

            result.add(new double[]{lat / 1e5, lng / 1e5});
        }
        return result;
    }

    private String toLatLng(double[] point) {
        return point[0] + "," + point[1];
    }

    private String toMode(TransportationMethod method) {
        return switch (method) {
            case WALKING -> "walking";
            case PUBLIC_TRANSIT -> "transit";
            case TAXI, SELF_DRIVING -> "driving";
        };
    }
}
