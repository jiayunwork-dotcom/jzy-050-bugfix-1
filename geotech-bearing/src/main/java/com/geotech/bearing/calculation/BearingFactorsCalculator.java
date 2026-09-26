package com.geotech.bearing.calculation;

import com.geotech.bearing.domain.BearingFactors;
import org.springframework.stereotype.Component;

/**
 * 承载力因子计算（太沙基经典闭合式）。
 *
 * <p><b>角度单位只在本类这一处从度转换为弧度</b>：{@code phiRad = toRadians(phiDeg)}。
 * 之后 Nq、Nc、Nγ 与下游三项叠加全部共用本次换算得到的同一组 φ，
 * 不存在因子那边用度、叠加那边用弧度对不上的可能。</p>
 *
 * <p>采用的闭合式：</p>
 * <ul>
 *   <li>Nq = e^(π·tanφ) · tan²(45° + φ/2)</li>
 *   <li>Nc = (Nq − 1) · cotφ</li>
 *   <li>Nγ = 2·(Nq + 1)·tanφ &nbsp;—— <b>Meyerhof（1963）表达式</b>。
 *       注意太沙基 Nγ 并无公认唯一闭式，常用表/教材数值分散；
 *       本服务固定采用上式并在此明确写明，保证结果可复现。φ=30° 时该式给出 Nγ≈22.40。</li>
 * </ul>
 *
 * <p><b>黏土极限（φ = 0 退化分支）</b>：cotφ 会除零，
 * 直接切换为黏土极限 Nc = 5.14、Nq = 1、Nγ = 0，绝不让 cot(0) 触发。
 * 5.14 即 (π + 2) 的常用近似（精确值约 5.1416）。</p>
 */
@Component
public class BearingFactorsCalculator {

    /** φ=0 黏土极限下的 Nc，取 (π+2) ≈ 5.14 的经典设计取值 */
    public static final double CLAY_NC = 5.14;
    public static final double CLAY_NQ = 1.0;
    public static final double CLAY_NGAMMA = 0.0;

    /**
     * 判定黏土极限的容差：|φ| 小于该值即按 φ=0 处理，避免极小角度下 cot 数值爆炸。
     * 合法输入 φ ≥ 0，这里用绝对值同时容忍浮点噪声。
     */
    private static final double PHI_ZERO_EPS = 1e-12;

    /**
     * 按「度」给出的内摩擦角计算三个承载力因子。
     *
     * @param frictionAngleDeg 内摩擦角 φ（度），调用前须已通过 0 ≤ φ &lt; 90 的校验
     * @return 同一组 φ 算出的 (Nc, Nq, Nγ)
     */
    public BearingFactors calculate(double frictionAngleDeg) {
        // 全服务唯一一处「度 → 弧度」换算，三个因子共用 phiRad。
        double phiRad = Math.toRadians(frictionAngleDeg);

        if (Math.abs(phiRad) < PHI_ZERO_EPS) {
            // 退化点：φ=0，切黏土极限，绕开 cot(0)。
            return new BearingFactors(CLAY_NC, CLAY_NQ, CLAY_NGAMMA);
        }

        double tanPhi = Math.tan(phiRad);
        double nq = Math.exp(Math.PI * tanPhi)
                * Math.pow(Math.tan(Math.PI / 4.0 + phiRad / 2.0), 2.0);
        double nc = (nq - 1.0) / tanPhi; // cotφ = 1/tanφ，此处 tanφ 已保证非零
        double ngamma = 2.0 * (nq + 1.0) * tanPhi; // Meyerhof(1963) 表达式

        return new BearingFactors(nc, nq, ngamma);
    }
}
