package com.geotech.bearing.validation;

import com.geotech.bearing.domain.FoundationGeometry;
import com.geotech.bearing.domain.SoilParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 输入校验测试：所有物理上不成立的取值都必须在算承载力因子之前被拒绝，并带原因码。
 */
class InputValidatorTest {

    private final InputValidator validator = new InputValidator();

    private InvalidInputException expectInvalid(Runnable r) {
        return assertThrows(InvalidInputException.class, r::run);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -0.5, -10})
    @DisplayName("宽度不为正 -> WIDTH_NOT_POSITIVE")
    void widthMustBePositive(double badWidth) {
        InvalidInputException ex = expectInvalid(() ->
                validator.validate(new FoundationGeometry(badWidth, 1.0)));
        assertEquals("WIDTH_NOT_POSITIVE", ex.getReason());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.01, -3})
    @DisplayName("埋深为负 -> DEPTH_NEGATIVE")
    void depthMustNotBeNegative(double badDepth) {
        InvalidInputException ex = expectInvalid(() ->
                validator.validate(new FoundationGeometry(2.0, badDepth)));
        assertEquals("DEPTH_NEGATIVE", ex.getReason());
    }

    @Test
    @DisplayName("埋深为零（地表基础）合法")
    void zeroDepthIsAllowed() {
        validator.validate(new FoundationGeometry(2.0, 0.0));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.1, -30})
    @DisplayName("内摩擦角为负 -> FRICTION_ANGLE_NEGATIVE")
    void frictionAngleMustNotBeNegative(double badPhi) {
        InvalidInputException ex = expectInvalid(() ->
                validator.validateFrictionAngle(badPhi));
        assertEquals("FRICTION_ANGLE_NEGATIVE", ex.getReason());
    }

    @ParameterizedTest
    @ValueSource(doubles = {90, 90.0001, 100})
    @DisplayName("φ 达到或超过 90 度 -> FRICTION_ANGLE_OUT_OF_RANGE（必须拦住 tan 发散）")
    void frictionAngleNinetyDegreesIsRejected(double badPhi) {
        InvalidInputException ex = expectInvalid(() ->
                validator.validateFrictionAngle(badPhi));
        assertEquals("FRICTION_ANGLE_OUT_OF_RANGE", ex.getReason());
    }

    @Test
    @DisplayName("φ 恰好略小于 90 度在角度校验层放行（数值发散由业务侧不触及该极端）")
    void angleJustBelowNinetyPassesAngleValidation() {
        validator.validateFrictionAngle(89.999);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -1, -18})
    @DisplayName("重度不为正 -> UNIT_WEIGHT_NOT_POSITIVE")
    void unitWeightMustBePositive(double badGamma) {
        InvalidInputException ex = expectInvalid(() ->
                validator.validate(new SoilParameters(10, 20, badGamma)));
        assertEquals("UNIT_WEIGHT_NOT_POSITIVE", ex.getReason());
    }

    @Test
    @DisplayName("黏聚力为负 -> COHESION_NEGATIVE")
    void cohesionMustNotBeNegative() {
        InvalidInputException ex = expectInvalid(() ->
                validator.validate(new SoilParameters(-5, 20, 18)));
        assertEquals("COHESION_NEGATIVE", ex.getReason());
    }

    @Test
    @DisplayName("c=0、φ=0 的纯黏性? 不——φ=0 且 γ>0 组合合法（黏土极限在计算层处理）")
    void zeroCohesionZeroFrictionIsValidInput() {
        validator.validate(new SoilParameters(0, 0, 18));
    }

    @Test
    @DisplayName("扫描区间：步长非正 -> SCAN_STEP_NOT_POSITIVE；上限小于下限 -> SCAN_RANGE_INVALID")
    void scanRangeValidation() {
        InvalidInputException stepEx = expectInvalid(() ->
                validator.validateScanRange(1, 5, 0));
        assertEquals("SCAN_STEP_NOT_POSITIVE", stepEx.getReason());

        InvalidInputException rangeEx = expectInvalid(() ->
                validator.validateScanRange(5, 1, 0.5));
        assertEquals("SCAN_RANGE_INVALID", rangeEx.getReason());
    }

    @Test
    @DisplayName("未知基础形状 -> SHAPE_UNKNOWN；空白缺省为条形")
    void shapeNormalization() {
        InvalidInputException ex = expectInvalid(() -> validator.normalizeShape("HEXAGON"));
        assertEquals("SHAPE_UNKNOWN", ex.getReason());
        assertTrue(Double.isFinite(validator.normalizeShape(null).ordinal()));
        assertEquals(com.geotech.bearing.domain.FoundationShape.STRIP,
                validator.normalizeShape(null));
    }
}
