package com.geotech.bearing.web;

import com.geotech.bearing.domain.SoilParameters;
import com.geotech.bearing.profile.SoilProfileEntity;
import com.geotech.bearing.profile.SoilProfileService;
import com.geotech.bearing.validation.InputValidator;
import com.geotech.bearing.web.dto.InlineSoilRequest;
import com.geotech.bearing.web.dto.ProfileResponse;
import com.geotech.bearing.web.dto.RegisterProfileRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 土层参数档的登记、查询、列出接口。参数档持久化，重启后仍可点名取用。
 */
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final SoilProfileService profileService;
    private final InputValidator validator;

    public ProfileController(SoilProfileService profileService, InputValidator validator) {
        this.profileService = profileService;
        this.validator = validator;
    }

    /** 登记一个新参数档；同名返回 409，物理上不成立的指标在落库前返回 400。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse register(@Valid @RequestBody RegisterProfileRequest request) {
        SoilParameters soil = WebDtoMapper.toSoilParameters(new InlineSoilRequest(
                request.cohesionKpa(), request.frictionAngleDeg(), request.unitWeightKnM3()));
        validator.validate(soil); // 登记时同样先校验，杜绝非法档入库
        SoilProfileEntity saved = profileService.register(
                request.name().trim(),
                soil.cohesionKpa(),
                soil.frictionAngleDeg(),
                soil.unitWeightKnM3(),
                request.description());
        return toResponse(saved);
    }

    /** 列出全部已登记参数档（按名称排序）。 */
    @GetMapping
    public List<ProfileResponse> list() {
        return profileService.list().stream().map(ProfileController::toResponse).toList();
    }

    /** 按名查询单个参数档；未登记返回 404 结构化错误。 */
    @GetMapping("/{name}")
    public ProfileResponse get(@PathVariable String name) {
        return toResponse(profileService.get(name));
    }

    private static ProfileResponse toResponse(SoilProfileEntity e) {
        return new ProfileResponse(
                e.getName(),
                e.getCohesionKpa(),
                e.getFrictionAngleDeg(),
                e.getUnitWeightKnM3(),
                e.getDescription(),
                e.getCreatedAt());
    }
}
