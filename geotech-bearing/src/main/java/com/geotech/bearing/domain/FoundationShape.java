package com.geotech.bearing.domain;

/**
 * 基础平面形状。缺省按条形（STRIP）处理；方形/圆形启用太沙基形状修正。
 */
public enum FoundationShape {
    /** 条形基础（默认，形状系数全为 1） */
    STRIP,
    /** 方形基础 */
    SQUARE,
    /** 圆形基础 */
    CIRCULAR
}
