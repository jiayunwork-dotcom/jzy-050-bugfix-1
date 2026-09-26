package com.geotech.bearing.web;

import com.geotech.bearing.domain.SoilParameters;
import com.geotech.bearing.validation.InvalidInputException;
import com.geotech.bearing.web.dto.InlineSoilRequest;

/**
 * 把 Web 层临时参数映射为领域值对象；字段缺失显式报「缺少」而不是任其 NPE。
 */
final class WebDtoMapper {

    private WebDtoMapper() {
    }

    static SoilParameters toSoilParameters(InlineSoilRequest req) {
        if (req == null) {
            throw new InvalidInputException("SOIL_PARAMETERS_MISSING",
                    "请求体中缺少 soil（全套土层参数）。");
        }
        if (req.cohesionKpa() == null) {
            throw new InvalidInputException("COHESION_MISSING", "缺少黏聚力 cohesionKpa。");
        }
        if (req.frictionAngleDeg() == null) {
            throw new InvalidInputException("FRICTION_ANGLE_MISSING", "缺少内摩擦角 frictionAngleDeg。");
        }
        if (req.unitWeightKnM3() == null) {
            throw new InvalidInputException("UNIT_WEIGHT_MISSING", "缺少土的重度 unitWeightKnM3。");
        }
        return new SoilParameters(req.cohesionKpa(), req.frictionAngleDeg(), req.unitWeightKnM3());
    }

    /** 几何字段缺失时给出明确的结构化错误（避免 NPE）。 */
    static double require(Double value, String field, String reason) {
        if (value == null || Double.isNaN(value)) {
            throw new InvalidInputException(reason, "缺少参数：" + field + "。");
        }
        return value;
    }
}
