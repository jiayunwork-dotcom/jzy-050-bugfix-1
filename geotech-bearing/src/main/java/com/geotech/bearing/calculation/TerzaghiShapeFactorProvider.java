package com.geotech.bearing.calculation;

import com.geotech.bearing.domain.FoundationShape;
import com.geotech.bearing.domain.ShapeFactorSet;
import org.springframework.stereotype.Component;

/**
 * 太沙基形状系数。
 *
 * <p>条形基础三项系数均为 1.0；方形/圆形基础按下列经典太沙基取值修正，
 * 三项（sc、sq、sγ）各自独立给出，计算时三项都要乘，不得只修其中一项：</p>
 * <ul>
 *   <li>方形：sc = 1.3，sq = 1.0，sγ = 0.8（即 1 − 0.2）</li>
 *   <li>圆形：sc = 1.3，sq = 1.0，sγ = 0.6（即 1 − 0.4）</li>
 * </ul>
 * <p>形状系数只与平面形状有关，与 φ 无关（太沙基体系）。</p>
 */
@Component
public class TerzaghiShapeFactorProvider {

    public ShapeFactorSet forShape(FoundationShape shape) {
        if (shape == null) {
            return ShapeFactorSet.strip();
        }
        return switch (shape) {
            case SQUARE -> new ShapeFactorSet(1.3, 1.0, 0.8);
            case CIRCULAR -> new ShapeFactorSet(1.3, 1.0, 0.6);
            case STRIP -> ShapeFactorSet.strip();
        };
    }
}
