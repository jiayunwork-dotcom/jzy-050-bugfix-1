package com.geotech.bearing.service;

import com.geotech.bearing.calculation.BearingFactorsCalculator;
import com.geotech.bearing.calculation.TerzaghiBearingCalculator;
import com.geotech.bearing.calculation.TerzaghiShapeFactorProvider;
import com.geotech.bearing.domain.BearingFactors;
import com.geotech.bearing.domain.BearingResult;
import com.geotech.bearing.domain.SoilParameters;
import com.geotech.bearing.profile.SoilProfileService;
import com.geotech.bearing.validation.InputValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 宽度扫描取点测试：锁定「每个点的宽度就是这一步该取的宽度」。
 *
 * <p>回归背景：扫描曾把每个宽度 {@code Math.round} 吸附到整数，
 * 导致 1.0→3.0、步长 0.5 的扫描跑出 1.0/2.0/2.0/3.0/3.0——1.5 与 2.5 被吞，
 * 2.0 与 3.0 各出现两次，c=0 砂土的 qu 在这两处原地踏步。这里把两条锁死：</p>
 * <ol>
 *   <li>宽度取点：小数步长下每个宽度精确落在 min+i·step 上，相邻间隔严格等于步长；</li>
 *   <li>承载力单调：c=0 砂土（只有自重项随宽度变化）qu 必须随宽度严格递增。</li>
 * </ol>
 */
class WidthScanServiceTest {

    /** 无黏聚力砂土：c=0、φ=30°、γ=19 kN/m³，qu 中只有自重项随 B 变化 */
    private static final SoilParameters SAND = new SoilParameters(0.0, 30.0, 19.0);

    private static final double EPS = 1e-12;

    private final SoilProfileService profileService = Mockito.mock(SoilProfileService.class);
    private final WidthScanService scanService = new WidthScanService(
            profileService,
            new TerzaghiBearingCalculator(
                    new BearingFactorsCalculator(),
                    new TerzaghiShapeFactorProvider()),
            new InputValidator());

    private List<BearingResult> scanSand(double min, double max, double step) {
        return scanService.scan(null, SAND, 1.0, "STRIP", min, max, step);
    }

    @Test
    @DisplayName("小数步长 0.5：宽度精确取 1.0/1.5/2.0/2.5/3.0，相邻间隔严格等于步长")
    void fractionalStepKeepsHalfMeterWidths() {
        List<BearingResult> points = scanSand(1.0, 3.0, 0.5);

        assertEquals(5, points.size(), "1.0→3.0、步长 0.5 应恰好 5 个点");
        double[] expected = {1.0, 1.5, 2.0, 2.5, 3.0};
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], points.get(i).geometry().widthM(), EPS,
                    "第 " + i + " 个点的宽度必须精确落在 min+i·step 上，不许向整数吸附");
        }
        // 区间端点：首点即下限、末点即上限
        assertEquals(1.0, points.get(0).geometry().widthM(), EPS);
        assertEquals(3.0, points.get(points.size() - 1).geometry().widthM(), EPS);
        // 相邻两点宽度间隔严格等于给定步长
        for (int i = 1; i < points.size(); i++) {
            assertEquals(0.5,
                    points.get(i).geometry().widthM() - points.get(i - 1).geometry().widthM(),
                    EPS, "相邻宽度间隔必须严格等于步长 0.5");
        }
    }

    @Test
    @DisplayName("c=0 砂土小数步长扫描：qu 随宽度严格递增，且逐点经得起手工核对")
    void cohesionlessSandQuStrictlyIncreasesWithWidth() {
        List<BearingResult> points = scanSand(1.0, 3.0, 0.5);

        BearingFactors factors = points.get(0).factors();
        double prevQu = -1.0;
        for (BearingResult p : points) {
            double b = p.geometry().widthM();
            // 手工核对：c=0、条形，qu = γ·Df·Nq + 0.5·γ·B·Nγ
            double expectedQu = 19.0 * 1.0 * factors.nq() + 0.5 * 19.0 * b * factors.ngamma();
            assertEquals(expectedQu, p.terms().qu(), 1e-9,
                    "B=" + b + " 的 qu 应等于手工核算值");
            assertEquals(0.0, p.terms().cohesionTerm(), EPS, "c=0 时黏聚力项必须为 0");
            // 同一土层同一 φ，各点三因子必须一致
            assertEquals(factors.nc(), p.factors().nc(), EPS);
            assertEquals(factors.nq(), p.factors().nq(), EPS);
            assertEquals(factors.ngamma(), p.factors().ngamma(), EPS);

            assertTrue(p.terms().qu() > prevQu,
                    "c=0 砂土 qu 必须随宽度严格递增，不许原地踏步：" + prevQu
                            + " -> " + p.terms().qu());
            prevQu = p.terms().qu();
        }
        // 相邻 qu 增量恰好等于自重项增量 0.5·γ·ΔB·Nγ（手工核对联动）
        double expectedDelta = 0.5 * 19.0 * 0.5 * factors.ngamma();
        for (int i = 1; i < points.size(); i++) {
            assertEquals(expectedDelta,
                    points.get(i).terms().qu() - points.get(i - 1).terms().qu(), 1e-9,
                    "相邻 qu 增量应等于 0.5·γ·step·Nγ");
        }
    }

    @Test
    @DisplayName("整数步长扫描保持原样：1→4、步长 1，宽度 1/2/3/4 共 4 点")
    void integerStepScanUnchanged() {
        List<BearingResult> points = scanSand(1.0, 4.0, 1.0);

        assertEquals(4, points.size());
        for (int i = 0; i < points.size(); i++) {
            assertEquals(1.0 + i, points.get(i).geometry().widthM(), EPS);
        }
    }

    @Test
    @DisplayName("0.1 步长不积累浮点噪声：宽度精确为 0.1/0.2/0.3，无 0.30000000000000004")
    void decimalStepDoesNotAccumulateFloatNoise() {
        List<BearingResult> points = scanSand(0.1, 0.3, 0.1);

        assertEquals(3, points.size());
        assertEquals(0.1, points.get(0).geometry().widthM(), 0.0);
        assertEquals(0.2, points.get(1).geometry().widthM(), 0.0);
        assertEquals(0.3, points.get(2).geometry().widthM(), 0.0);
    }

    @Test
    @DisplayName("步长不能整除区间：取点不越出上限（1→3、步长 0.7 得 1.0/1.7/2.4）")
    void nonDividingStepStopsBeforeExceedingMax() {
        List<BearingResult> points = scanSand(1.0, 3.0, 0.7);

        assertEquals(3, points.size());
        assertEquals(1.0, points.get(0).geometry().widthM(), EPS);
        assertEquals(1.7, points.get(1).geometry().widthM(), EPS);
        assertEquals(2.4, points.get(2).geometry().widthM(), EPS);
        for (BearingResult p : points) {
            assertTrue(p.geometry().widthM() <= 3.0 + EPS, "取点不得越出区间上限");
        }
    }
}
