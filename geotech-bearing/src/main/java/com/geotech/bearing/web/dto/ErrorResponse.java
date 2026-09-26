package com.geotech.bearing.web.dto;

import java.time.Instant;

/**
 * 统一结构化错误响应。
 *
 * @param timestamp 发生时间
 * @param status    HTTP 状态码
 * @param error     机器可读原因码（如 WIDTH_NOT_POSITIVE、PROFILE_NOT_FOUND）
 * @param message   中文人类说明
 * @param path      请求路径
 */
public record ErrorResponse(Instant timestamp,
                            int status,
                            String error,
                            String message,
                            String path) {
}
