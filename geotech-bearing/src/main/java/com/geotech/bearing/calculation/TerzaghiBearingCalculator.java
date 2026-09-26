package com.geotech.bearing.calculation;

import com.geotech.bearing.domain.BearingFactors;
import com.geotech.bearing.domain.BearingResult;
import com.geotech.bearing.domain.BearingTerms;
import com.geotech.bearing.domain.FoundationGeometry;
import com.geotech.bearing.domain.ShapeFactorSet;
import com.geotech.bearing.domain.SoilParameters;
import org.springframework.stereotype.Component;

/**
 * 太沙基条形基础极限承载力三项叠加。
 *
 * <pre>qu = c·Nc·sc + γ·Df·Nq·sq + 0.5·γ·B·Nγ·sγ</pre>
 *
 * 三项依次为：黏聚力项、超载（埋深）项、自重（宽度）项。
 * 承载力因子统一来自 {@link BearingFactorsCalculator}（内部只做一次度→弧度换算），
 * 形状系数统一来自 {@link TerzaghiShapeFactorProvider}，
 * 本类只负责在同一组 φ 的因子上做分项与叠加，确保口径一致。
 */
@Component
public class TerzaghiBearingCalculator {

    private final BearingFactorsCalculator factorsCalculator;
    private final TerzaghiShapeFactorProvider shapeFactorProvider;

    public TerzaghiBearingCalculator(BearingFactorsCalculator factorsCalculator,
                                     TerzaghiShapeFactorProvider shapeFactorProvider) {
        this.factorsCalculator = factorsCalculator;
        this.shapeFactorProvider = shapeFactorProvider;
    }

    /**
     * 完成一次完整核算。
     *
     * @param soil     已通过校验的土层参数（c、φ、γ）
     * @param geometry 已通过校验的基础条件（B、Df、形状）
     */
    public BearingResult calculate(SoilParameters soil, FoundationGeometry geometry) {
        // 因子在一处算出，分项叠加共用同一对象，φ 换算不可能错位。
        BearingFactors factors = factorsCalculator.calculate(soil.frictionAngleDeg());
        ShapeFactorSet shapeFactors = shapeFactorProvider.forShape(geometry.shape());

        double c = soil.cohesionKpa();
        double gamma = soil.unitWeightKnM3();
        double df = geometry.depthM();
        double b = geometry.widthM();

        // 三项各自带各自的形状修正，方形/圆形象证：三项都乘，不许漏项。
        double cohesionTerm = c * factors.nc() * shapeFactors.sc();
        double surchargeTerm = gamma * df * factors.nq() * shapeFactors.sq();
        double weightTerm = 0.5 * gamma * b * factors.ngamma() * shapeFactors.sGamma();

        double qu = cohesionTerm + surchargeTerm + weightTerm;

        return new BearingResult(
                soil,
                geometry,
                factors,
                shapeFactors,
                new BearingTerms(cohesionTerm, surchargeTerm, weightTerm, qu));
    }
}
