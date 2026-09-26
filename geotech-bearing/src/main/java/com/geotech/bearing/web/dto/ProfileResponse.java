package com.geotech.bearing.web.dto;

import java.time.Instant;

/**
 * 参数档对外视图。
 */
public record ProfileResponse(String name,
                              double cohesionKpa,
                              double frictionAngleDeg,
                              double unitWeightKnM3,
                              String description,
                              Instant createdAt) {
}
