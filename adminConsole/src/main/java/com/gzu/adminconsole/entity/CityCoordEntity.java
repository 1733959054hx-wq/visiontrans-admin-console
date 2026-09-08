package com.gzu.adminconsole.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 城市坐标缓存：地理编码结果落库，避免每次渲染地图都外呼地理编码 API。
 */
@Entity
@Table(name = "city_coord_cache")
public class CityCoordEntity {

    /** 城市名（业务主键）。 */
    @Id
    private String city;

    /** 经度。 */
    private double lon;

    /** 纬度。 */
    private double lat;

    /** 数据来源：tencent-lbs / open-meteo。 */
    private String source;

    protected CityCoordEntity() {
    }

    public CityCoordEntity(String city, double lon, double lat, String source) {
        this.city = city;
        this.lon = lon;
        this.lat = lat;
        this.source = source;
    }

    public String getCity() {
        return city;
    }

    public double getLon() {
        return lon;
    }

    public double getLat() {
        return lat;
    }

    public String getSource() {
        return source;
    }
}
