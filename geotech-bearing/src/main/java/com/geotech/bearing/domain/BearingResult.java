package com.geotech.bearing.domain;

/**
 * 一次完整核算的结果：承载力因子、分项、总和与入参回响（便于手工核对）。
 *
 * @param soilParameters 实际参与计算的土层参数（点名参数档时为档内值）
 * @param geometry       实际参与计算的基础条件
 * @param factors        三个承载力因子
 * @param shapeFactors   本次使用的三项形状系数（条形时全为 1）
 * @param terms          形状修正后的三项分项与 qu
 */
public record BearingResult(SoilParameters soilParameters,
                            FoundationGeometry geometry,
                            BearingFactors factors,
                            ShapeFactorSet shapeFactors,
                            BearingTerms terms) {

    public double qu() {
        return terms.qu();
    }
}
