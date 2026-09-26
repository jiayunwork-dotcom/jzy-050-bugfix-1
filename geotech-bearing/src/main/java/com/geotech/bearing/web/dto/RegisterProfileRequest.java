package com.geotech.bearing.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登记一个土层参数档的请求体。
 */
public record RegisterProfileRequest(
        @NotBlank(message = "参数档名 name 不能为空")
        String name,
        Double cohesionKpa,
        Double frictionAngleDeg,
        Double unitWeightKnM3,
        String description) {
}
