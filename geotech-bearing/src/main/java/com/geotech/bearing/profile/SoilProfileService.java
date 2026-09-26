package com.geotech.bearing.profile;

import com.geotech.bearing.domain.SoilParameters;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 土层参数档的登记、查询、列出与点名取用。
 *
 * <p>所有写操作在单个数据库事务内完成；{@code name} 上有唯一约束兜底，
 * 即使并发登记同名也只有一笔成功，其余转成 {@link ProfileAlreadyExistsException}，
 * 不会出现两份同名档互相写串。</p>
 */
@Service
public class SoilProfileService {

    private final SoilProfileRepository repository;

    public SoilProfileService(SoilProfileRepository repository) {
        this.repository = repository;
    }

    /**
     * 匹配唯一约束冲突（PostgreSQL：unique_violation；H2：Unique index ... violation；
     * 以及通用 duplicate key 文案）。
     */
    private static final Pattern UNIQUE_VIOLATION = Pattern.compile(
            "(?i)unique[_ ]constraint|unique[_ ]index|unique_violation|duplicate key");

    @Transactional
    public SoilProfileEntity register(String name, double cohesionKpa, double frictionAngleDeg,
                                      double unitWeightKnM3, String description) {
        if (repository.existsByName(name)) {
            throw new ProfileAlreadyExistsException(name);
        }
        SoilProfileEntity entity =
                new SoilProfileEntity(name, cohesionKpa, frictionAngleDeg, unitWeightKnM3, description);
        try {
            return repository.saveAndFlush(entity);
        } catch (RuntimeException ex) {
            // 并发登记同名时由 name 唯一约束兜底；不同驱动包装层级不一，按根因文案识别。
            if (isUniqueNameViolation(ex)) {
                throw new ProfileAlreadyExistsException(name);
            }
            throw ex;
        }
    }

    private boolean isUniqueNameViolation(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof DataIntegrityViolationException) {
                return true;
            }
            String msg = t.getMessage();
            if (msg != null && UNIQUE_VIOLATION.matcher(msg).find()) {
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<SoilProfileEntity> list() {
        return repository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public SoilProfileEntity get(String name) {
        return repository.findByName(name)
                .orElseThrow(() -> new ProfileNotFoundException(name));
    }

    /** 点名取用并还原为计算内核使用的土层参数值对象。 */
    @Transactional(readOnly = true)
    public SoilParameters requireParameters(String name) {
        SoilProfileEntity entity = get(name);
        return new SoilParameters(
                entity.getCohesionKpa(),
                entity.getFrictionAngleDeg(),
                entity.getUnitWeightKnM3());
    }
}
