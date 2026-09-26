package com.geotech.bearing.domain;

/**
 * 一套完整的土层抗剪与重度指标。
 *
 * @param cohesionKpa      黏聚力 c，单位 kPa
 * @param frictionAngleDeg 内摩擦角 φ，单位「度」，合法区间 [0, 90)
 * @param unitWeightKnM3   土的重度 γ，单位 kN/m³
 */
public record SoilParameters(double cohesionKpa,
                             double frictionAngleDeg,
                             double unitWeightKnM3) {
}
