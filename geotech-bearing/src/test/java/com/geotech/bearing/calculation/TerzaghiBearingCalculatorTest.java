package com.geotech.bearing.calculation;

import com.geotech.bearing.domain.BearingResult;
import com.geotech.bearing.domain.FoundationGeometry;
import com.geotech.bearing.domain.FoundationShape;
import com.geotech.bearing.domain.ShapeFactorSet;
import com.geotech.bearing.domain.SoilParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 太沙基三项叠加 + 形状修正测试。
 *
 * <p>逐条锁定需求中的联动关系：</p>
 * <ol>
 *   <li>φ=0 且 c&gt;0：只剩黏聚力项与超载项，自重项为 0；</li>
 *   <li>c=0：第一项消失，qu 随宽度与埋深变化；</li>
 *   <li>宽度加倍：自重项加倍；</li>
 *   <li>埋深加倍：超载项加倍；</li>
 *   <li>提高 φ：qu 上升；</li>
 *   <li>方形/圆形：三项都带各自形状修正。</li>
 * </ol>
 */
class TerzaghiBearingCalculatorTest {

    private final TerzaghiBearingCalculator calculator =
            new TerzaghiBearingCalculator(
                    new BearingFactorsCalculator(),
                    new TerzaghiShapeFactorProvider());

    private static final double EPS = 1e-9;

    private BearingResult calc(double c, double phi, double gamma,
                               double b, double df, FoundationShape shape) {
        return calculator.calculate(
                new SoilParameters(c, phi, gamma),
                new FoundationGeometry(b, df, shape));
    }

    @Test
    @DisplayName("φ=0、c>0：黏土极限，Nγ=0，只剩黏聚力项与超载项")
    void clayLimitOnlyCohesionAndSurchargeContribute() {
        double c = 30, phi = 0, gamma = 18, b = 2, df = 1.5;
        BearingResult r = calc(c, phi, gamma, b, df, FoundationShape.STRIP);

        assertEquals(5.14, r.factors().nc(), EPS);
        assertEquals(0.0, r.factors().ngamma(), EPS);
        assertEquals(c * 5.14, r.terms().cohesionTerm(), 1e-9, "黏聚力项 = c·Nc");
        assertEquals(gamma * df * 1.0, r.terms().surchargeTerm(), 1e-9, "超载项 = γ·Df·Nq，Nq=1");
        assertEquals(0.0, r.terms().weightTerm(), EPS, "自重项必须为 0（Nγ=0）");
        assertEquals(c * 5.14 + gamma * df, r.terms().qu(), 1e-9,
                "qu 仅由黏聚力项与超载项构成");
    }

    @Test
    @DisplayName("c=0：黏聚力项消失，qu 随宽度与埋深变化")
    void zeroCohesionDropsFirstTerm() {
        double phi = 25, gamma = 19;
        BearingResult r = calc(0, phi, gamma, 2, 1, FoundationShape.STRIP);

        assertEquals(0.0, r.terms().cohesionTerm(), EPS, "c=0 时黏聚力项必须为 0");
        assertTrue(r.terms().surchargeTerm() > 0, "超载项应为正");
        assertTrue(r.terms().weightTerm() > 0, "自重项应为正");
        assertEquals(r.terms().surchargeTerm() + r.terms().weightTerm(),
                r.terms().qu(), 1e-9);
    }

    @Test
    @DisplayName("单独把宽度加倍：仅自重项随之加倍，其余两项不变")
    void doublingWidthDoublesWeightTermOnly() {
        double c = 10, phi = 20, gamma = 18, df = 1.2;
        BearingResult base = calc(c, phi, gamma, 2.0, df, FoundationShape.STRIP);
        BearingResult wide = calc(c, phi, gamma, 4.0, df, FoundationShape.STRIP);

        assertEquals(base.terms().cohesionTerm(), wide.terms().cohesionTerm(), EPS,
                "宽度变化不影响黏聚力项");
        assertEquals(base.terms().surchargeTerm(), wide.terms().surchargeTerm(), EPS,
                "宽度变化不影响超载项");
        assertEquals(2.0, wide.terms().weightTerm() / base.terms().weightTerm(), 1e-12,
                "宽度加倍，自重项必须精确加倍");
        // qu 的增量应恰好等于自重项的增量
        assertEquals(wide.terms().weightTerm() - base.terms().weightTerm(),
                wide.terms().qu() - base.terms().qu(), 1e-9);
    }

    @Test
    @DisplayName("单独把埋深加倍：仅超载项随之加倍，其余两项不变")
    void doublingDepthDoublesSurchargeTermOnly() {
        double c = 10, phi = 20, gamma = 18, b = 2.5;
        BearingResult base = calc(c, phi, gamma, b, 1.0, FoundationShape.STRIP);
        BearingResult deep = calc(c, phi, gamma, b, 2.0, FoundationShape.STRIP);

        assertEquals(base.terms().cohesionTerm(), deep.terms().cohesionTerm(), EPS,
                "埋深变化不影响黏聚力项");
        assertEquals(base.terms().weightTerm(), deep.terms().weightTerm(), EPS,
                "埋深变化不影响自重项");
        assertEquals(2.0, deep.terms().surchargeTerm() / base.terms().surchargeTerm(), 1e-12,
                "埋深加倍，超载项必须精确加倍");
    }

    @Test
    @DisplayName("单独提高内摩擦角：Nq 上升、qu 上升")
    void increasingFrictionAngleRaisesNqAndQu() {
        double c = 5, gamma = 19, b = 2, df = 1;
        BearingResult low = calc(c, 15, gamma, b, df, FoundationShape.STRIP);
        BearingResult high = calc(c, 30, gamma, b, df, FoundationShape.STRIP);

        assertTrue(high.factors().nq() > low.factors().nq(), "Nq 应随 φ 上升");
        assertTrue(high.terms().qu() > low.terms().qu(), "qu 应随 φ 上升");
    }

    @Test
    @DisplayName("方形基础：三项都乘各自形状系数（sc=1.3, sq=1, sγ=0.8）")
    void squareShapeFactorsAppliedToAllThreeTerms() {
        double c = 20, phi = 25, gamma = 19, b = 3, df = 1.5;
        BearingResult strip = calc(c, phi, gamma, b, df, FoundationShape.STRIP);
        BearingResult square = calc(c, phi, gamma, b, df, FoundationShape.SQUARE);

        ShapeFactorSet s = square.shapeFactors();
        assertEquals(1.3, s.sc(), EPS);
        assertEquals(1.0, s.sq(), EPS);
        assertEquals(0.8, s.sGamma(), EPS);

        // 关键：三项都必须按各自形状系数修正，不许只修一项
        assertEquals(strip.terms().cohesionTerm() * 1.3, square.terms().cohesionTerm(), 1e-9,
                "方形：黏聚力项乘 sc=1.3");
        assertEquals(strip.terms().surchargeTerm() * 1.0, square.terms().surchargeTerm(), 1e-9,
                "方形：超载项乘 sq=1.0");
        assertEquals(strip.terms().weightTerm() * 0.8, square.terms().weightTerm(), 1e-9,
                "方形：自重项乘 sγ=0.8");
        assertEquals(
                square.terms().cohesionTerm()
                        + square.terms().surchargeTerm()
                        + square.terms().weightTerm(),
                square.terms().qu(), 1e-9);
    }

    @Test
    @DisplayName("圆形基础：三项都乘各自形状系数（sc=1.3, sq=1, sγ=0.6）")
    void circularShapeFactorsAppliedToAllThreeTerms() {
        double c = 20, phi = 25, gamma = 19, b = 3, df = 1.5;
        BearingResult strip = calc(c, phi, gamma, b, df, FoundationShape.STRIP);
        BearingResult circle = calc(c, phi, gamma, b, df, FoundationShape.CIRCULAR);

        assertEquals(1.3, circle.shapeFactors().sc(), EPS);
        assertEquals(0.6, circle.shapeFactors().sGamma(), EPS);
        assertEquals(strip.terms().cohesionTerm() * 1.3, circle.terms().cohesionTerm(), 1e-9);
        assertEquals(strip.terms().surchargeTerm(), circle.terms().surchargeTerm(), 1e-9);
        assertEquals(strip.terms().weightTerm() * 0.6, circle.terms().weightTerm(), 1e-9);
    }

    @Test
    @DisplayName("条形缺省：形状系数全为 1，三项不做额外修正")
    void stripDefaultsToUnityShapeFactors() {
        BearingResult r = calc(10, 20, 18, 2, 1, FoundationShape.STRIP);
        assertEquals(1.0, r.shapeFactors().sc(), EPS);
        assertEquals(1.0, r.shapeFactors().sq(), EPS);
        assertEquals(1.0, r.shapeFactors().sGamma(), EPS);
    }
}
