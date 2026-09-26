package com.geotech.bearing.profile;

import com.geotech.bearing.calculation.BearingFactorsCalculator;
import com.geotech.bearing.domain.BearingFactors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 内置示范参数档初始化：中密砂土。
 *
 * <p>φ=30°、c=0、γ=19 kN/m³。该档算出的承载力因子接近文献表值
 * （Nc≈30.1、Nq≈18.4，Nγ 采用 Meyerhof 式 ≈20.1），
 * 且 c=0 使第一项消失，qu 随宽度增大而增大，便于手工核对。</p>
 *
 * <p>幂等：启动时若已存在（数据库持久化 / 重复启动）则跳过。</p>
 */
@Component
public class DemoProfileInitializer implements ApplicationRunner {

    /** 内置示范参数档名 */
    public static final String DEMO_PROFILE_NAME = "medium-dense-sand";

    private static final Logger log = LoggerFactory.getLogger(DemoProfileInitializer.class);

    private final SoilProfileService profileService;
    private final SoilProfileRepository repository;
    private final BearingFactorsCalculator factorsCalculator;

    public DemoProfileInitializer(SoilProfileService profileService,
                                  SoilProfileRepository repository,
                                  BearingFactorsCalculator factorsCalculator) {
        this.profileService = profileService;
        this.repository = repository;
        this.factorsCalculator = factorsCalculator;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.existsByName(DEMO_PROFILE_NAME)) {
            log.info("示范参数档 '{}' 已存在，跳过初始化。", DEMO_PROFILE_NAME);
            return;
        }
        double c = 0.0;
        double phi = 30.0;
        double gamma = 19.0;
        profileService.register(DEMO_PROFILE_NAME, c, phi, gamma,
                "内置示范：中密砂土，φ=30°，c=0，γ=19 kN/m³；条形基础承载力因子接近文献表值。");

        BearingFactors f = factorsCalculator.calculate(phi);
        log.info("已登记内置示范参数档 '{}'（c=0, φ=30°, γ=19）-> Nc={}, Nq={}, Nγ={}",
                DEMO_PROFILE_NAME,
                String.format("%.2f", f.nc()),
                String.format("%.2f", f.nq()),
                String.format("%.2f", f.ngamma()));
    }
}
