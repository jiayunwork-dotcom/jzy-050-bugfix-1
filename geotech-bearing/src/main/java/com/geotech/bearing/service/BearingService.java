package com.geotech.bearing.service;

import com.geotech.bearing.calculation.BearingFactorsCalculator;
import com.geotech.bearing.calculation.TerzaghiBearingCalculator;
import com.geotech.bearing.domain.BearingFactors;
import com.geotech.bearing.domain.BearingResult;
import com.geotech.bearing.domain.FoundationGeometry;
import com.geotech.bearing.domain.SoilParameters;
import com.geotech.bearing.profile.SoilProfileService;
import com.geotech.bearing.validation.InputValidator;
import org.springframework.stereotype.Service;

/**
 * 核算编排：解析「点名参数档 / 临时全套参数」两种来源，先校验后计算。
 *
 * <p>计算本身（因子、黏土极限、三项叠加、形状修正）全部下沉在 calculation 包，
 * 本类只负责参数来源解析与调用顺序，保持单一职责。</p>
 */
@Service
public class BearingService {

    private final SoilProfileService profileService;
    private final BearingFactorsCalculator factorsCalculator;
    private final TerzaghiBearingCalculator bearingCalculator;
    private final InputValidator validator;

    public BearingService(SoilProfileService profileService,
                          BearingFactorsCalculator factorsCalculator,
                          TerzaghiBearingCalculator bearingCalculator,
                          InputValidator validator) {
        this.profileService = profileService;
        this.factorsCalculator = factorsCalculator;
        this.bearingCalculator = bearingCalculator;
        this.validator = validator;
    }

    /** 单独按内摩擦角（度）查询三个承载力因子；先做角度校验，挡住 φ≥90°。 */
    public BearingFactors factors(double frictionAngleDeg) {
        validator.validateFrictionAngle(frictionAngleDeg);
        return factorsCalculator.calculate(frictionAngleDeg);
    }

    /**
     * 一次完整核算。
     *
     * @param profileName      已登记参数档名；非空时以该档为准
     * @param inline           临时给出的全套土层参数；profileName 为空时必须完整
     * @param widthM           基础宽度 B，m
     * @param depthM           埋深 Df，m
     * @param shape            平面形状（null/空 按条形 STRIP）
     */
    public BearingResult calculate(String profileName,
                                   SoilParameters inline,
                                   double widthM,
                                   double depthM,
                                   String shape) {
        SoilParameters soil = resolveSoil(profileName, inline);
        FoundationGeometry geometry =
                new FoundationGeometry(widthM, depthM, validator.normalizeShape(shape));

        // 先校验（在任何承载力因子计算之前），再计算。
        validator.validate(soil);
        validator.validate(geometry);

        return bearingCalculator.calculate(soil, geometry);
    }

    private SoilParameters resolveSoil(String profileName, SoilParameters inline) {
        boolean named = profileName != null && !profileName.isBlank();
        if (named) {
            // 点名未登记档会抛 ProfileNotFoundException，绝不拿默认值凑结果。
            return profileService.requireParameters(profileName.trim());
        }
        if (inline == null) {
            throw new com.geotech.bearing.validation.InvalidInputException(
                    "SOIL_PARAMETERS_MISSING",
                    "必须点名一个已登记参数档（profileName），或在请求体内临时给出全套土层参数。");
        }
        return inline;
    }
}
