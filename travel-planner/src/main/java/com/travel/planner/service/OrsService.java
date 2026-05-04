package com.travel.planner.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.planner.dto.response.TransitStepInfo;
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
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrsService {

    private static final Logger log = LoggerFactory.getLogger(OrsService.class);
    private static final String GMAPS_BASE = "https://maps.googleapis.com/maps/api";

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
            String url = GMAPS_BASE + "/geocode/json?address=" + encoded + "&key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            String status = root.path("status").asText();
            if (!"OK".equals(status)) {
                log.warn("Geocoding 找不到地址: {}，狀態: {}", address, status);
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
     * latLngPoints: [[lat, lng], ...]；addresses: 對應的原始地址字串（transit 專用）
     * transit 模式用地址字串避免 geocoded_waypoints 為空的問題，且不支援 waypoints 改為逐段呼叫
     */
    public OrsRouteResult getDirections(List<double[]> latLngPoints, List<String> addresses, TransportationMethod method) {
        String mode = toGoogleMode(method);
        if ("transit".equals(mode)) {
            return getDirectionsSegmented(latLngPoints, addresses, mode);
        }
        return getDirectionsSingleCall(latLngPoints, mode);
    }

    private OrsRouteResult getDirectionsSingleCall(List<double[]> latLngPoints, String mode) {
        try {
            double[] origin = latLngPoints.get(0);
            double[] destination = latLngPoints.get(latLngPoints.size() - 1);

            StringBuilder urlBuilder = new StringBuilder(GMAPS_BASE + "/directions/json?");
            urlBuilder.append("origin=").append(origin[0]).append(",").append(origin[1]);
            urlBuilder.append("&destination=").append(destination[0]).append(",").append(destination[1]);
            urlBuilder.append("&mode=").append(mode);

            if (latLngPoints.size() > 2) {
                urlBuilder.append("&waypoints=");
                for (int i = 1; i < latLngPoints.size() - 1; i++) {
                    if (i > 1) urlBuilder.append("|");
                    urlBuilder.append(latLngPoints.get(i)[0]).append(",").append(latLngPoints.get(i)[1]);
                }
            }

            urlBuilder.append("&key=").append(apiKey);

            JsonNode root = sendGet(urlBuilder.toString());
            String status = root.path("status").asText();
            if (!"OK".equals(status)) {
                log.warn("Directions API 回傳 {} [mode={}] origin={},{} destination={},{}",
                        status, mode, origin[0], origin[1], destination[0], destination[1]);
                throw new InvalidInputException(toDirectionsError(status));
            }

            JsonNode route = root.path("routes").get(0);
            JsonNode legs = route.path("legs");

            double totalDistance = 0;
            double totalDuration = 0;
            List<double[]> legData = new ArrayList<>();

            for (JsonNode leg : legs) {
                double legDist = leg.path("distance").path("value").asDouble();
                double legDur = leg.path("duration").path("value").asDouble();
                totalDistance += legDist;
                totalDuration += legDur;
                legData.add(new double[]{legDist, legDur});
            }

            List<double[]> geometry = decodePolyline(route.path("overview_polyline").path("points").asText());
            return new OrsRouteResult(totalDistance, totalDuration, geometry, legData, null, null);

        } catch (InvalidInputException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidInputException("路線計算失敗：" + e.getMessage());
        }
    }

    // transit 模式不支援 waypoints，逐段呼叫後合併
    private OrsRouteResult getDirectionsSegmented(List<double[]> latLngPoints, List<String> addresses, String mode) {
        try {
            long departureTime = nextTransitDepartureTime();
            double totalDistance = 0;
            double totalDuration = 0;
            List<double[]> geometry = new ArrayList<>();
            List<double[]> legData = new ArrayList<>();
            List<Double> legFares = new ArrayList<>();
            List<List<TransitStepInfo>> segmentSteps = new ArrayList<>();

            for (int i = 0; i < latLngPoints.size() - 1; i++) {
                String originParam = encodeAddress(addresses, i, latLngPoints.get(i));
                String destParam   = encodeAddress(addresses, i + 1, latLngPoints.get(i + 1));

                String url = GMAPS_BASE + "/directions/json?"
                        + "origin=" + originParam
                        + "&destination=" + destParam
                        + "&mode=" + mode
                        + "&departure_time=" + departureTime
                        + "&key=" + apiKey;

                JsonNode root = sendGet(url);
                String status = root.path("status").asText();

                if (!"OK".equals(status)) {
                    String errorMsg = root.path("error_message").asText("(無)");
                    log.warn("Directions API 回傳 {} [transit] segment {} error_message={}", status, i, errorMsg);
                    throw new InvalidInputException(toDirectionsError(status));
                }

                JsonNode route = root.path("routes").get(0);
                JsonNode leg = route.path("legs").get(0);

                // 確認回應中有真正的大眾運輸步驟，避免 Google 回傳純步行路線
                boolean hasTransitStep = false;
                for (JsonNode step : leg.path("steps")) {
                    if ("TRANSIT".equals(step.path("travel_mode").asText())) {
                        hasTransitStep = true;
                        break;
                    }
                }
                if (!hasTransitStep) {
                    throw new InvalidInputException(
                        "「" + (addresses != null && i < addresses.size() ? addresses.get(i) : "起點") + "」到「" +
                        (addresses != null && i + 1 < addresses.size() ? addresses.get(i + 1) : "終點") + "」之間找不到大眾運輸路線，距離可能過短或無直達路線，建議改用步行。"
                    );
                }

                double legDist = leg.path("distance").path("value").asDouble();
                double legDur = leg.path("duration").path("value").asDouble();
                totalDistance += legDist;
                totalDuration += legDur;
                legData.add(new double[]{legDist, legDur});

                // 取 Google 回傳的實際票價（transit 模式才有）
                JsonNode fareNode = route.path("fare");
                Double legFare = fareNode.isMissingNode() ? null : fareNode.path("value").asDouble();
                legFares.add(legFare);

                // 擷取每個步驟的交通方式詳情（捷運線名、公車號碼、站名等）
                List<TransitStepInfo> steps = new ArrayList<>();
                for (JsonNode step : leg.path("steps")) {
                    String stepMode = step.path("travel_mode").asText();
                    int stepMin = (int) Math.round(step.path("duration").path("value").asDouble() / 60.0);
                    if ("TRANSIT".equals(stepMode)) {
                        JsonNode td = step.path("transit_details");
                        String vehicleName = td.path("line").path("vehicle").path("name").asText("大眾運輸");
                        String lineName    = td.path("line").path("name").asText("");
                        String depStop     = td.path("departure_stop").path("name").asText("");
                        String arrStop     = td.path("arrival_stop").path("name").asText("");
                        int numStops       = td.path("num_stops").asInt(0);
                        steps.add(new TransitStepInfo("TRANSIT", vehicleName, lineName,
                                depStop, arrStop, numStops, stepMin));
                    } else {
                        // WALKING 段（轉乘步行）
                        steps.add(new TransitStepInfo("WALKING", "步行", null,
                                null, null, null, stepMin));
                    }
                }
                segmentSteps.add(steps);

                List<double[]> segGeometry = decodePolyline(route.path("overview_polyline").path("points").asText());
                if (geometry.isEmpty()) {
                    geometry.addAll(segGeometry);
                } else if (!segGeometry.isEmpty()) {
                    // 避免重複起點
                    geometry.addAll(segGeometry.subList(1, segGeometry.size()));
                }
            }

            return new OrsRouteResult(totalDistance, totalDuration, geometry, legData, legFares, segmentSteps);

        } catch (InvalidInputException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidInputException("路線計算失敗：" + e.getMessage());
        }
    }

    /**
     * 計算多個起點到多個終點的距離與時間矩陣，使用 Google Distance Matrix API
     * 回傳 result[i][j] = [distanceMeters, durationSeconds]
     */
    public double[][][] getDistanceMatrix(List<double[]> origins, List<double[]> destinations, TransportationMethod method) {
        try {
            String mode = toGoogleMode(method);
            String originsStr = URLEncoder.encode(buildCoordList(origins), StandardCharsets.UTF_8);
            String destinationsStr = URLEncoder.encode(buildCoordList(destinations), StandardCharsets.UTF_8);

            String url = GMAPS_BASE + "/distancematrix/json?origins=" + originsStr
                    + "&destinations=" + destinationsStr
                    + "&mode=" + mode
                    + "&key=" + apiKey;

            JsonNode root = sendGet(url);
            String status = root.path("status").asText();
            if (!"OK".equals(status)) {
                throw new InvalidInputException("Google Distance Matrix 失敗（" + status + "）");
            }

            JsonNode rows = root.path("rows");
            double[][][] result = new double[rows.size()][][];
            for (int i = 0; i < rows.size(); i++) {
                JsonNode elements = rows.get(i).path("elements");
                result[i] = new double[elements.size()][];
                for (int j = 0; j < elements.size(); j++) {
                    JsonNode element = elements.get(j);
                    double dist = element.path("distance").path("value").asDouble();
                    double dur = element.path("duration").path("value").asDouble();
                    result[i][j] = new double[]{dist, dur};
                }
            }
            return result;

        } catch (InvalidInputException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidInputException("Distance Matrix 計算失敗：" + e.getMessage());
        }
    }

    private JsonNode sendGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readTree(response.body());
    }

    private String toDirectionsError(String status) {
        return switch (status) {
            case "ZERO_RESULTS" -> "找不到路線，所選景點之間可能沒有該交通方式的路線";
            case "NOT_FOUND" -> "找不到起點或終點，請確認景點地址是否正確";
            case "MAX_WAYPOINTS_EXCEEDED" -> "景點數量超過上限，請減少選擇的景點數量";
            case "REQUEST_DENIED" -> "API Key 無效或未啟用 Directions API";
            default -> "Google Directions 失敗（" + status + "）";
        };
    }

    /** 將地址 URL encode（空白用 %20），無地址時 fallback 到座標 */
    private String encodeAddress(List<String> addresses, int idx, double[] latLng) {
        if (addresses != null && idx < addresses.size() && addresses.get(idx) != null) {
            return URLEncoder.encode(addresses.get(idx), StandardCharsets.UTF_8).replace("+", "%20");
        }
        return latLng[0] + "," + latLng[1];
    }

    /**
     * 回傳適合 transit 規劃的出發時間（東京時區）：
     * - 06:00–22:00 → 現在 + 10 分鐘（有班次的時段）
     * - 22:00–06:00 → 隔天 09:00（末班後/首班前，避免 ZERO_RESULTS）
     */
    private long nextTransitDepartureTime() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"));
        LocalTime time = now.toLocalTime();
        ZonedDateTime departure;
        if (time.isBefore(LocalTime.of(6, 0)) || time.isAfter(LocalTime.of(22, 0))) {
            ZonedDateTime base = time.isAfter(LocalTime.of(22, 0)) ? now.plusDays(1) : now;
            departure = base.withHour(9).withMinute(0).withSecond(0).withNano(0);
        } else {
            departure = now.plusMinutes(10);
        }
        return departure.toEpochSecond();
    }

    private String buildCoordList(List<double[]> points) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) sb.append("|");
            sb.append(points.get(i)[0]).append(",").append(points.get(i)[1]);
        }
        return sb.toString();
    }

    private String toGoogleMode(TransportationMethod method) {
        return switch (method) {
            case WALKING -> "walking";
            case PUBLIC_TRANSIT -> "transit";
            case TAXI -> "driving";
        };
    }

    /**
     * 解碼 Google Encoded Polyline 格式為 [lat, lng] 座標列表
     */
    private List<double[]> decodePolyline(String encoded) {
        List<double[]> path = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);
            lng += dlng;

            path.add(new double[]{lat / 1e5, lng / 1e5});
        }
        return path;
    }
}
