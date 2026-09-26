package com.geotech.bearing.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 具名「土层参数档」实体 —— 本服务的一等公民。
 *
 * <p>每一档完整描述一套土的抗剪与重度指标（c、φ、γ），持久化在 PostgreSQL 中，
 * 进程重启后仍可点名取用。{@code name} 唯一，作为点名取用的键。</p>
 */
@Entity
@Table(name = "soil_profiles")
public class SoilProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    /** 黏聚力 c，kPa */
    @Column(name = "cohesion_kpa", nullable = false)
    private double cohesionKpa;

    /** 内摩擦角 φ，度 */
    @Column(name = "friction_angle_deg", nullable = false)
    private double frictionAngleDeg;

    /** 土的重度 γ，kN/m³ */
    @Column(name = "unit_weight_kn_m3", nullable = false)
    private double unitWeightKnM3;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected SoilProfileEntity() {
        // JPA 要求
    }

    public SoilProfileEntity(String name, double cohesionKpa, double frictionAngleDeg,
                             double unitWeightKnM3, String description) {
        this.name = name;
        this.cohesionKpa = cohesionKpa;
        this.frictionAngleDeg = frictionAngleDeg;
        this.unitWeightKnM3 = unitWeightKnM3;
        this.description = description;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getCohesionKpa() {
        return cohesionKpa;
    }

    public double getFrictionAngleDeg() {
        return frictionAngleDeg;
    }

    public double getUnitWeightKnM3() {
        return unitWeightKnM3;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
