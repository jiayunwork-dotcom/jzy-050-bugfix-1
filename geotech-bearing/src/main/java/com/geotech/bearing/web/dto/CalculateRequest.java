package com.geotech.bearing.web.dto;

/**
 * 一次核算请求：profileName 与 soil 二选一（点名已登记档，或临时给全套参数）。
 */
public record CalculateRequest(String profileName,
                               InlineSoilRequest soil,
                               Double widthM,
                               Double depthM,
                               String shape) {
}
