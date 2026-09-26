package com.geotech.bearing.web.dto;

/**
 * 临时全套土层参数（点名参数档时可不填）。
 * 字段使用包装类型，缺字段时由映射逻辑显式报「缺少」而不是 NPE。
 */
public record InlineSoilRequest(Double cohesionKpa,
                                Double frictionAngleDeg,
                                Double unitWeightKnM3) {
}
