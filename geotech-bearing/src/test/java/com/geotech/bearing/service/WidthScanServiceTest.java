package com.geotech.bearing.service;

import com.geotech.bearing.calculation.BearingFactorsCalculator;
import com.geotech.bearing.calculation.TerzaghiBearingCalculator;
import com.geotech.bearing.calculation.TerzaghiShapeFactorProvider;
import com.geotech.bearing.domain.BearingResult;
import com.geotech.bearing.domain.SoilParameters;
import com.geotech.bearing.profile.SoilProfileService;
import com.geotech.bearing.validation.InputValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * 宽度扫描取点逻辑测试（不依赖 Spring 上下文，全部走临时参数，不触碰参数档）。
 *
 * <p>锁定三条不变量：</p>
 * <ol>
 *   <li>每个点的宽度是本步真正该取的宽度——小数步长下 1.5 就是 1.5，
 *       绝不向就近整数吸附，也不带 3.0000000000000004 式漂移；</li>
 *   <li>相邻两点宽度间隔严格等于给定步长，端点不多不少；</li>
 *   <li>c=0 砂土只有自重项随宽度变化，qu 必须随宽度严格递增。</li>
 * </ol>
 */
class WidthScanServiceTest {

    private static final double EPS = 1e-12;

    /** c=0、φ=30°、γ=19 的砂土：三项中只剩超载项与自重项 */
    private static final SoilParameters SAND = new SoilParameters(0.0, 30.0, 19.0);

    private final WidthScanService service = new WidthScanService(
            mock(SoilProfileService.class),
            new TerzaghiBearingCalculator(
                    new BearingFactorsCalculator(), new TerzaghiShapeFactorProvider()),
            new InputValidator());

    private List<BearingResult> scanInline(double min, double max, double step) {
        return service.scan(null, SAND, 1.0, "STRIP", min, max, step);
    }

    @Test
    @DisplayName("小数步长 0.5：宽度取点 1.0/1.5/2.0/2.5/3.0 不被对齐到整数，qu 严格递增")
    void fractionalStepKeepsExactWidthsAndStrictlyIncreasingQu() {
        List<BearingResult> points = scanInline(1.0, 3.0, 0.5);

        assertEquals(5, points.size(), "1.0~3.0 每 0.5 一点应得 5 个点");
        double[] expectedWidths = {1.0, 1.5, 2.0, 2.5, 3.0};
        double prevQu = -1;
        for (int i = 0; i < expectedWidths.length; i++) {
            double width = points.get(i).geometry().widthM();
            assertEquals(expectedWidths[i], width, EPS,
                    "第 " + i + " 点的宽度必须是 " + expectedWidths[i] + "，不得就近对齐到整数");
            if (i > 0) {
                assertEquals(0.5, width - expectedWidths[i - 1], EPS,
                        "相邻两点宽度间隔必须严格等于步长 0.5");
            }
            // 手工核对：c=0 砂土 qu = γ·Df·Nq + 0.5·γ·B·Nγ（按文献闭式独立复算）
            double qu = points.get(i).qu();
            assertEquals(expectedSandQu(19.0, 1.0, width, 30.0), qu, 1e-9,
                    "qu 应等于 γ·Df·Nq + 0.5·γ·B·Nγ");
            assertTrue(qu > prevQu,
                    "c=0 砂土 qu 必须随宽度严格递增，不得原地踏步：" + prevQu + " -> " + qu);
            prevQu = qu;
        }
        // 端点：首点即下限、末点即上限
        assertEquals(1.0, points.get(0).geometry().widthM(), EPS);
        assertEquals(3.0, points.get(points.size() - 1).geometry().widthM(), EPS);
    }

    @Test
    @DisplayName("整数步长回归：1~4 每 1 一点，仍精确取 1/2/3/4 四点")
    void integerStepStillExact() {
        List<BearingResult> points = scanInline(1.0, 4.0, 1.0);

        assertEquals(4, points.size());
        double[] expectedWidths = {1.0, 2.0, 3.0, 4.0};
        for (int i = 0; i < expectedWidths.length; i++) {
            assertEquals(expectedWidths[i], points.get(i).geometry().widthM(), EPS);
        }
    }

    @Test
    @DisplayName("0.1 步长无浮点漂移：宽度精确为 1.0,1.1,…,1.5，末点落在区间上限")
    void decimalStepHasNoFloatDrift() {
        List<BearingResult> points = scanInline(1.0, 1.5, 0.1);

        assertEquals(6, points.size(), "1.0~1.5 每 0.1 一点应得 6 个点");
        double[] expectedWidths = {1.0, 1.1, 1.2, 1.3, 1.4, 1.5};
        for (int i = 0; i < expectedWidths.length; i++) {
            assertEquals(expectedWidths[i], points.get(i).geometry().widthM(), EPS,
                    "宽度不得出现 1.3000000000000003 一类漂移");
            if (i > 0) {
                assertEquals(0.1,
                        points.get(i).geometry().widthM() - points.get(i - 1).geometry().widthM(),
                        EPS, "相邻两点宽度间隔必须严格等于步长 0.1");
            }
        }
    }

    @Test
    @DisplayName("区间不是步长整数倍：取点不越上限，末点为区间内最后一个步点")
    void rangeNotMultipleOfStepStopsWithinRange() {
        List<BearingResult> points = scanInline(1.0, 3.2, 0.5);

        assertEquals(5, points.size(), "1.0~3.2 每 0.5 一点应取到 3.0 为止");
        double[] expectedWidths = {1.0, 1.5, 2.0, 2.5, 3.0};
        for (int i = 0; i < expectedWidths.length; i++) {
            assertEquals(expectedWidths[i], points.get(i).geometry().widthM(), EPS);
            assertTrue(points.get(i).geometry().widthM() <= 3.2, "取点不得越过区间上限");
        }
    }

    /** 按文献闭式独立复算 c=0 砂土的 qu，用于手工核对联动。 */
    private static double expectedSandQu(double gamma, double df, double b, double phiDeg) {
        double phiRad = Math.toRadians(phiDeg);
        double tanPhi = Math.tan(phiRad);
        double nq = Math.exp(Math.PI * tanPhi)
                * Math.pow(Math.tan(Math.PI / 4.0 + phiRad / 2.0), 2.0);
        double ngamma = 2.0 * (nq + 1.0) * tanPhi;
        return gamma * df * nq + 0.5 * gamma * b * ngamma;
    }
}
