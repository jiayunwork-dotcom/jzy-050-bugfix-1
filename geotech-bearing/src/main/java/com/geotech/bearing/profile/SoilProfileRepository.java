package com.geotech.bearing.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 参数档关系型存取。Spring Data 在 PostgreSQL 上提供事务边界与行级读写，
 * 多档登记 / 多次核算并发进来时各自操作独立行，结果互不写串。
 */
public interface SoilProfileRepository extends JpaRepository<SoilProfileEntity, Long> {

    Optional<SoilProfileEntity> findByName(String name);

    boolean existsByName(String name);

    List<SoilProfileEntity> findAllByOrderByNameAsc();
}
