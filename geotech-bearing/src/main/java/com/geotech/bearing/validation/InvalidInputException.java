package com.geotech.bearing.validation;

/**
 * 物理上不成立的输入在「计算承载力因子之前」被拒绝时抛出。
 * 携带机器可读的 {@link #reason} 与中文人类说明，由全局异常处理转成结构化错误响应。
 */
public class InvalidInputException extends RuntimeException {

    /** 错误原因码，例如 WIDTH_NOT_POSITIVE、FRICTION_ANGLE_OUT_OF_RANGE */
    private final String reason;

    public InvalidInputException(String reason, String message) {
        super(message);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
