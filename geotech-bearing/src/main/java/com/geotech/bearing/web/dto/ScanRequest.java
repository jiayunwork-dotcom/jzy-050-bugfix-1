package com.geotech.bearing.web.dto;

/**
 * 宽度扫描请求：固定土层参数、埋深与形状，宽度在区间内按步长变化。
 */
public record ScanRequest(String profileName,
                          InlineSoilRequest soil,
                          Double depthM,
                          String shape,
                          Double minWidthM,
                          Double maxWidthM,
                          Double stepM) {
}
