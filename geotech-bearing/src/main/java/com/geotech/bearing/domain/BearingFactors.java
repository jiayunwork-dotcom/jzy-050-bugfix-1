package com.geotech.bearing.domain;

/**
 * 三个太沙基承载力因子，由同一组 φ（弧度）一次性算出，三项叠加共用本对象。
 *
 * @param nc     Nc —— 黏聚力项因子
 * @param nq     Nq —— 超载（埋深）项因子
 * @param ngamma Nγ —— 自重（宽度）项因子
 */
public record BearingFactors(double nc, double nq, double ngamma) {
}
