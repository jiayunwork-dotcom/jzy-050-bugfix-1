package com.geotech.bearing.web;

import com.geotech.bearing.domain.BearingFactors;
import com.geotech.bearing.domain.BearingResult;
import com.geotech.bearing.domain.SoilParameters;
import com.geotech.bearing.service.BearingService;
import com.geotech.bearing.service.WidthScanService;
import com.geotech.bearing.web.dto.BearingResultResponse;
import com.geotech.bearing.web.dto.CalculateRequest;
import com.geotech.bearing.web.dto.ScanRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 承载力核算接口：
 * <ul>
 *   <li>POST /api/bearings/calculate —— 对一组基础条件做一次核算（点名档或临时参数）</li>
 *   <li>GET  /api/bearings/factors   —— 单独按内摩擦角查询三个承载力因子</li>
 *   <li>POST /api/bearings/scan      —— 宽度区间扫描，返回 qu 随宽度变化点列</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/bearings")
public class BearingController {

    private final BearingService bearingService;
    private final WidthScanService widthScanService;

    public BearingController(BearingService bearingService, WidthScanService widthScanService) {
        this.bearingService = bearingService;
        this.widthScanService = widthScanService;
    }

    @PostMapping("/calculate")
    public BearingResultResponse calculate(@RequestBody CalculateRequest request) {
        SoilParameters inline = request.soil() == null
                ? null
                : WebDtoMapper.toSoilParameters(request.soil());
        double width = WebDtoMapper.require(request.widthM(), "widthM", "WIDTH_MISSING");
        double depth = WebDtoMapper.require(request.depthM(), "depthM", "DEPTH_MISSING");

        boolean named = request.profileName() != null && !request.profileName().isBlank();
        BearingResult result = bearingService.calculate(
                request.profileName(), inline, width, depth, request.shape());

        return BearingResultResponse.from(result,
                named ? "PROFILE" : "INLINE",
                named ? request.profileName().trim() : null);
    }

    /** 单独按内摩擦角（度）查询因子。φ 为负或 ≥90° 返回结构化错误。 */
    @GetMapping("/factors")
    public BearingFactorsResponse factors(@RequestParam("frictionAngleDeg") Double frictionAngleDeg) {
        if (frictionAngleDeg == null) {
            // 该分支通常被 MissingServletRequestParameterException 先拦截，留作双保险。
            throw new com.geotech.bearing.validation.InvalidInputException(
                    "FRICTION_ANGLE_MISSING", "缺少查询参数 frictionAngleDeg。");
        }
        BearingFactors f = bearingService.factors(frictionAngleDeg);
        return new BearingFactorsResponse(frictionAngleDeg, f.nc(), f.nq(), f.ngamma());
    }

    @PostMapping("/scan")
    public ScanResponse scan(@RequestBody ScanRequest request) {
        SoilParameters inline = request.soil() == null
                ? null
                : WebDtoMapper.toSoilParameters(request.soil());
        double depth = WebDtoMapper.require(request.depthM(), "depthM", "DEPTH_MISSING");
        double minWidth = WebDtoMapper.require(request.minWidthM(), "minWidthM", "WIDTH_MISSING");
        double maxWidth = WebDtoMapper.require(request.maxWidthM(), "maxWidthM", "WIDTH_MISSING");
        double step = WebDtoMapper.require(request.stepM(), "stepM", "STEP_MISSING");

        boolean named = request.profileName() != null && !request.profileName().isBlank();
        List<BearingResult> points = widthScanService.scan(
                request.profileName(), inline, depth, request.shape(),
                minWidth, maxWidth, step);

        List<BearingResultResponse> pointViews = points.stream()
                .map(p -> BearingResultResponse.from(p, named ? "PROFILE" : "INLINE",
                        named ? request.profileName().trim() : null))
                .toList();
        return new ScanResponse(pointViews.size(), pointViews);
    }

    /** GET /factors 的响应视图。 */
    public record BearingFactorsResponse(double frictionAngleDeg,
                                         double nc,
                                         double nq,
                                         double ngamma) {
    }

    /** POST /scan 的响应视图：点数 + 点列。 */
    public record ScanResponse(int count, List<BearingResultResponse> points) {
    }
}
