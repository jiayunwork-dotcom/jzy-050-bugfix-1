package com.geotech.bearing.domain;

/**
 * 三项各自的形状系数（sc, sq, sγ）。即使取 1.0 也显式持有，
 * 保证方形/圆形修正时黏聚力、超载、自重三项都经过各自的形状修正，
 * 不允许只修其中一项。
 *
 * @param sc        黏聚力项形状系数
 * @param sq        超载项形状系数
 * @param sGamma    自重项形状系数
 */
public record ShapeFactorSet(double sc, double sq, double sGamma) {

    public static ShapeFactorSet strip() {
        return new ShapeFactorSet(1.0, 1.0, 1.0);
    }
}
