package com.gzu.adminconsole.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.gzu.adminconsole.entity.CityCoordEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 城市地理编码服务：把城市名动态解析为经纬度，替代硬编码坐标表。
 *
 * <p>解析顺序：
 * <ol>
 *   <li>数据库缓存 {@code city_coord_cache}（解析成功一次后不再外呼）</li>
 *   <li>腾讯位置服务地理编码（配置了 {@code admin-console.security.tencent-lbs-key} 时优先）</li>
 *   <li>Open-Meteo 免 Key 地理编码（兜底，无任何依赖）</li>
 * </ol>
 * 外网不可用或解析失败时返回 null，调用方自行跳过该城市，不影响其余数据。
 */
@Service
public class GeocodingService {

    private static final String TENCENT_GEOCODER_URL = "https://apis.map.qq.com/ws/geocoder/v1/?address=%s&key=%s";
    private static final String OPEN_METEO_GEOCODER_URL = "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1&language=zh&format=json";

    @PersistenceContext
    private EntityManager em;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    /** 腾讯位置服务 Key（可选，控制台 https://lbs.qq.com 申请）。 */
    @Value("${admin-console.security.tencent-lbs-key:}")
    private String tencentLbsKey;

    /**
     * 解析城市坐标。
     *
     * @return [lon, lat]，无法解析时返回 null
     */
    @Transactional
    public double[] resolve(String city) {
        if (city == null || city.isBlank()) {
            return null;
        }
        CityCoordEntity cached = em.find(CityCoordEntity.class, city);
        if (cached != null) {
            return new double[] {cached.getLon(), cached.getLat()};
        }

        double[] coord = null;
        String source = null;
        if (tencentLbsKey != null && !tencentLbsKey.isBlank()) {
            coord = fromTencent(city);
            source = "tencent-lbs";
        }
        if (coord == null) {
            coord = fromOpenMeteo(city);
            source = "open-meteo";
        }
        if (coord == null) {
            return null;
        }
        em.persist(new CityCoordEntity(city, coord[0], coord[1], source));
        return coord;
    }

    /** 腾讯位置服务：https://lbs.qq.com/service/webService/webServiceGuide/webServiceGeocoder */
    private double[] fromTencent(String city) {
        try {
            String url = String.format(TENCENT_GEOCODER_URL,
                    URLEncoder.encode(city, StandardCharsets.UTF_8),
                    URLEncoder.encode(tencentLbsKey, StandardCharsets.UTF_8));
            JsonNode root = mapper.readTree(http.send(HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .GET().build(), HttpResponse.BodyHandlers.ofString()).body());
            JsonNode location = root.path("result").path("location");
            if (root.path("status").asInt(-1) == 0 && location.has("lat") && location.has("lng")) {
                return new double[] {location.get("lng").asDouble(), location.get("lat").asDouble()};
            }
        } catch (Exception ignored) {
            // 外网异常 / 配额超限，交给兜底源处理
        }
        return null;
    }

    /** Open-Meteo 免 Key 地理编码：https://geocoding-api.open-meteo.com */
    private double[] fromOpenMeteo(String city) {
        try {
            String url = String.format(OPEN_METEO_GEOCODER_URL,
                    URLEncoder.encode(city, StandardCharsets.UTF_8));
            JsonNode root = mapper.readTree(http.send(HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .GET().build(), HttpResponse.BodyHandlers.ofString()).body());
            JsonNode first = root.path("results").path(0);
            if (first.has("longitude") && first.has("latitude")) {
                return new double[] {first.get("longitude").asDouble(), first.get("latitude").asDouble()};
            }
        } catch (Exception ignored) {
            // 双源均失败，返回 null 由调用方跳过
        }
        return null;
    }
}
