package com.geotech.bearing.domain;

/**
 * 基础几何条件。
 *
 * @param widthM 条形基础宽度 B（方形取边长、圆形取直径），单位 m，必须为正
 * @param depthM 基础埋深 Df，单位 m，不允许为负
 * @param shape  平面形状，null 视为条形
 */
public record FoundationGeometry(double widthM,
                                 double depthM,
                                 FoundationShape shape) {

    public FoundationGeometry(double widthM, double depthM) {
        this(widthM, depthM, FoundationShape.STRIP);
    }
}
