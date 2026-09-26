package com.geotech.bearing.service;

import com.geotech.bearing.calculation.TerzaghiBearingCalculator;
import com.geotech.bearing.domain.BearingResult;
import com.geotech.bearing.domain.FoundationGeometry;
import com.geotech.bearing.domain.FoundationShape;
import com.geotech.bearing.domain.SoilParameters;
import com.geotech.bearing.profile.SoilProfileService;
import com.geotech.bearing.validation.InputValidator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 宽度扫描：固定土层参数与埋深（及形状），让基础宽度在 [minWidth, maxWidth]
 * 区间按 step 变化，逐点核算，返回 qu 随宽度变化的点列，供观察加宽趋势。
 *
 * <p>每次步进都复用同一个 {@link TerzaghiBearingCalculator}，仅改变宽度 B；
 * 因为三项中只有自重项含 B，c=0 的砂土可直接看到 qu 随 B 线性增大。</p>
 */
@Service
public class WidthScanService {

    /** 扫描点数的保护性上限，防止超大区间/极小步长造成资源耗尽 */
    private static final int MAX_POINTS = 10_000;

    private final SoilProfileService profileService;
    private final TerzaghiBearingCalculator bearingCalculator;
    private final InputValidator validator;

    public WidthScanService(SoilProfileService profileService,
                            TerzaghiBearingCalculator bearingCalculator,
                            InputValidator validator) {
        this.profileService = profileService;
        this.bearingCalculator = bearingCalculator;
        this.validator = validator;
    }

    public List<BearingResult> scan(String profileName,
                                    SoilParameters inline,
                                    double depthM,
                                    String shape,
                                    double minWidthM,
                                    double maxWidthM,
                                    double stepM) {
        SoilParameters soil = resolveSoil(profileName, inline);
        FoundationShape foundationShape = validator.normalizeShape(shape);

        validator.validate(soil);
        validator.validate(new FoundationGeometry(1.0, depthM, foundationShape));
        validator.validateScanRange(minWidthM, maxWidthM, stepM);

        List<BearingResult> points = new ArrayList<>();
        int guard = 0;
        // 用整数步数枚举，避免 0.1 之类步长的浮点漂移导致漏点/多点或宽度出现 3.0000000004。
        long steps = Math.round((maxWidthM - minWidthM) / stepM);
        for (long i = 0; i <= steps && guard < MAX_POINTS; i++, guard++) {
            double width = minWidthM + i * stepM;
            // 整数步点吸附回「期望值」（如 1、2、3），消除 3.0000000000000004 一类噪声。
            width = Math.round(width);
            if (width > maxWidthM + 1e-9) {
                break;
            }
            FoundationGeometry geometry = new FoundationGeometry(width, depthM, foundationShape);
            points.add(bearingCalculator.calculate(soil, geometry));
        }
        return points;
    }

    private SoilParameters resolveSoil(String profileName, SoilParameters inline) {
        boolean named = profileName != null && !profileName.isBlank();
        if (named) {
            return profileService.requireParameters(profileName.trim());
        }
        if (inline == null) {
            throw new com.geotech.bearing.validation.InvalidInputException(
                    "SOIL_PARAMETERS_MISSING",
                    "扫描必须点名一个已登记参数档（profileName），或临时给出全套土层参数。");
        }
        return inline;
    }
}
