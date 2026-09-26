package com.geotech.bearing.validation;

import com.geotech.bearing.domain.FoundationGeometry;
import com.geotech.bearing.domain.FoundationShape;
import com.geotech.bearing.domain.SoilParameters;
import org.springframework.stereotype.Component;

/**
 * 进入承载力计算之前的物理合法性校验，独立成模块。
 *
 * <p>任何不成立的取值都在「算承载力因子之前」抛 {@link InvalidInputException}，
 * 特别地 φ ≥ 90° 必须拦住——否则 tan(45°+45°)=tan90° 发散；φ &lt; 0 同样拒绝。</p>
 */
@Component
public class InputValidator {

    /** φ 的合法区间为 [0, 90) 度，达到 90 度即不合法。 */
    public static final double MAX_FRICTION_ANGLE_EXCLUSIVE = 90.0;

    public void validate(SoilParameters soil) {
        if (soil == null) {
            throw new InvalidInputException("SOIL_PARAMETERS_MISSING",
                    "缺少土层参数：需要完整给出黏聚力、内摩擦角与重度。");
        }
        if (Double.isNaN(soil.cohesionKpa()) || soil.cohesionKpa() < 0.0) {
            throw new InvalidInputException("COHESION_NEGATIVE",
                    "黏聚力不能为负：c=" + soil.cohesionKpa() + " kPa。");
        }
        validateFrictionAngle(soil.frictionAngleDeg());
        if (Double.isNaN(soil.unitWeightKnM3()) || soil.unitWeightKnM3() <= 0.0) {
            throw new InvalidInputException("UNIT_WEIGHT_NOT_POSITIVE",
                    "土的重度必须为正：γ=" + soil.unitWeightKnM3() + " kN/m³。");
        }
    }

    /** 仅按内摩擦角查因子时也走同一道角度校验。 */
    public void validateFrictionAngle(Double frictionAngleDeg) {
        if (frictionAngleDeg == null || Double.isNaN(frictionAngleDeg)) {
            throw new InvalidInputException("FRICTION_ANGLE_MISSING",
                    "缺少内摩擦角 φ。");
        }
        if (frictionAngleDeg < 0.0) {
            throw new InvalidInputException("FRICTION_ANGLE_NEGATIVE",
                    "内摩擦角不能为负：φ=" + frictionAngleDeg + "°。");
        }
        if (frictionAngleDeg >= MAX_FRICTION_ANGLE_EXCLUSIVE) {
            // 必须在调用 tan(45°+φ/2) 之前拦住，φ=90° 会使其发散。
            throw new InvalidInputException("FRICTION_ANGLE_OUT_OF_RANGE",
                    "内摩擦角必须小于 90°（达到 90° 会使承载力因子发散）：φ="
                            + frictionAngleDeg + "°。");
        }
    }

    public void validate(FoundationGeometry geometry) {
        if (geometry == null) {
            throw new InvalidInputException("GEOMETRY_MISSING",
                    "缺少基础几何条件：需要给出宽度与埋深。");
        }
        if (Double.isNaN(geometry.widthM()) || geometry.widthM() <= 0.0) {
            throw new InvalidInputException("WIDTH_NOT_POSITIVE",
                    "基础宽度必须为正：B=" + geometry.widthM() + " m。");
        }
        if (Double.isNaN(geometry.depthM()) || geometry.depthM() < 0.0) {
            throw new InvalidInputException("DEPTH_NEGATIVE",
                    "基础埋深不能为负：Df=" + geometry.depthM() + " m。");
        }
    }

    /**
     * 宽度扫描区间校验。
     *
     * @param minWidthM 区间下限（含），必须为正
     * @param maxWidthM 区间上限（含），必须不小于下限
     * @param stepM     步长，必须为正
     */
    public void validateScanRange(double minWidthM, double maxWidthM, double stepM) {
        if (Double.isNaN(minWidthM) || minWidthM <= 0.0) {
            throw new InvalidInputException("WIDTH_NOT_POSITIVE",
                    "扫描区间下限宽度必须为正：minWidth=" + minWidthM + " m。");
        }
        if (Double.isNaN(maxWidthM) || maxWidthM <= 0.0) {
            throw new InvalidInputException("WIDTH_NOT_POSITIVE",
                    "扫描区间上限宽度必须为正：maxWidth=" + maxWidthM + " m。");
        }
        if (maxWidthM < minWidthM) {
            throw new InvalidInputException("SCAN_RANGE_INVALID",
                    "扫描区间上限不能小于下限：minWidth=" + minWidthM
                            + " m，maxWidth=" + maxWidthM + " m。");
        }
        if (Double.isNaN(stepM) || stepM <= 0.0) {
            throw new InvalidInputException("SCAN_STEP_NOT_POSITIVE",
                    "扫描步长必须为正：step=" + stepM + " m。");
        }
    }

    public FoundationShape normalizeShape(String shape) {
        if (shape == null || shape.isBlank()) {
            return FoundationShape.STRIP;
        }
        try {
            return FoundationShape.valueOf(shape.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidInputException("SHAPE_UNKNOWN",
                    "未知基础形状 '" + shape + "'，可选：STRIP、SQUARE、CIRCULAR。");
        }
    }
}
