package com.geotech.bearing.calculation;

import com.geotech.bearing.domain.BearingFactors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 承载力因子计算内核测试。
 *
 * <p>重点守：φ=0 黏土极限退化分支（绝不让 cot(0) 触发）、φ=30° 接近文献表值、
 * Nq/Nγ 随 φ 单调上升。</p>
 */
class BearingFactorsCalculatorTest {

    private final BearingFactorsCalculator calculator = new BearingFactorsCalculator();

    private static final double EPS = 1e-9;

    @Test
    @DisplayName("φ=0：切换黏土极限 Nc=5.14、Nq=1、Nγ=0（不除零）")
    void clayLimitAtZeroFrictionAngle() {
        BearingFactors f = calculator.calculate(0.0);

        assertEquals(5.14, f.nc(), EPS, "φ=0 时 Nc 取黏土极限 5.14");
        assertEquals(1.0, f.nq(), EPS, "φ=0 时 Nq=1");
        assertEquals(0.0, f.ngamma(), EPS, "φ=0 时 Nγ=0");
        assertTrue(Double.isFinite(f.nc()) && Double.isFinite(f.nq()) && Double.isFinite(f.ngamma()),
                "黏土极限下三个因子都必须是有限值，不能出现 Infinity/NaN");
    }

    @Test
    @DisplayName("φ=30°：Nc≈30.14、Nq≈18.40、Nγ≈22.40，接近经典文献表值")
    void factorsAtThirtyDegreesMatchLiterature() {
        BearingFactors f = calculator.calculate(30.0);

        // 文献表值：Nq=18.40，Nc=30.14；Nγ 采用 Meyerhof 式 2(Nq+1)tanφ ≈ 22.40
        assertEquals(18.40, f.nq(), 0.02, "Nq 应接近文献表值 18.40");
        assertEquals(30.14, f.nc(), 0.05, "Nc 应接近文献表值 30.14");
        assertEquals(22.40, f.ngamma(), 0.1, "Nγ(Meyerhof) 应约为 22.40");
    }

    @Test
    @DisplayName("提高 φ：Nq、Nc、Nγ 全部严格上升")
    void factorsIncreaseWithFrictionAngle() {
        double[] angles = {0, 5, 10, 15, 20, 25, 30, 35, 40};
        for (int i = 1; i < angles.length; i++) {
            BearingFactors prev = calculator.calculate(angles[i - 1]);
            BearingFactors cur = calculator.calculate(angles[i]);
            assertTrue(cur.nq() > prev.nq(),
                    "Nq 应随 φ 上升：" + angles[i - 1] + "->" + angles[i]);
            assertTrue(cur.nc() > prev.nc(),
                    "Nc 应随 φ 上升：" + angles[i - 1] + "->" + angles[i]);
            assertTrue(cur.ngamma() >= prev.ngamma(),
                    "Nγ 应随 φ 不减：" + angles[i - 1] + "->" + angles[i]);
        }
    }

    @Test
    @DisplayName("φ 接近 0 的极小正角仍给出有限结果（cot 数值稳定）")
    void tinyPositiveAngleStaysFinite() {
        BearingFactors f = calculator.calculate(1e-6);
        assertTrue(Double.isFinite(f.nc()) && Double.isFinite(f.nq()) && Double.isFinite(f.ngamma()),
                "极小 φ 下因子应全部有限");
        // 与黏土极限应非常接近
        assertEquals(5.14, f.nc(), 0.01);
    }
}
