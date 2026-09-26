package com.geotech.bearing.domain;

/**
 * 形状修正后的三项分项结果与极限承载力总和。
 *
 * <p>三项分别对应：黏聚力项 c·Nc·sc、超载项 γ·Df·Nq·sq、自重项 0.5·γ·B·Nγ·sγ。</p>
 *
 * @param cohesionTerm  黏聚力项，kPa
 * @param surchargeTerm 超载（埋深）项，kPa
 * @param weightTerm    自重（宽度）项，kPa
 * @param qu            地基极限承载力 qu（三项之和），kPa
 */
public record BearingTerms(double cohesionTerm,
                           double surchargeTerm,
                           double weightTerm,
                           double qu) {
}
