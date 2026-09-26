package com.geotech.bearing.profile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import com.geotech.bearing.BearingCapacityServiceApplication;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 参数档持久化测试：参数档必须落到关系型数据库，进程（Spring 上下文）重启后仍可点名取用。
 *
 * <p>测试中用文件模式 H2（PostgreSQL 兼容）作为关系库，连续启动两个独立上下文：
 * 第一个上下文登记一个参数档，关闭；第二个上下文重新打开同一个库文件并点名查询。</p>
 */
class PersistenceAcrossRestartTest {

    @TempDir
    Path tempDir;

    private ConfigurableApplicationContext startContext(String dbFile) {
        // 用命令行参数（最高优先级属性源）覆盖 application.yml 里的 PostgreSQL 数据源，
        // 指向同一个 H2 文件库，模拟两次进程启动复用同一关系库。
        String[] args = {
                "--spring.datasource.url=jdbc:h2:file:" + dbFile
                        + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "--spring.datasource.driver-class-name=org.h2.Driver",
                "--spring.datasource.username=sa",
                "--spring.datasource.password=",
                "--spring.jpa.hibernate.ddl-auto=update",
                "--spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
                "--spring.main.web-application-type=none",
                "--logging.level.root=WARN",
                "--logging.level.com.geotech=WARN"
        };
        return new SpringApplicationBuilder(BearingCapacityServiceApplication.class)
                .run(args);
    }

    @Test
    @DisplayName("参数档在上下文重启后仍可点名取用（持久化到关系库）")
    void profileSurvivesRestart() {
        String dbFile = tempDir.resolve("geotech-persist").toString();
        String profileName = "stiff-clay-persist";

        // 第一个「进程」：登记后关闭
        try (ConfigurableApplicationContext first = startContext(dbFile)) {
            SoilProfileService service = first.getBean(SoilProfileService.class);
            SoilProfileEntity saved = service.register(profileName, 42.0, 0.0, 19.5,
                    "持久化测试用硬黏土");
            assertNotNull(saved.getId());
            assertTrue(saved.getId() > 0);
        }

        // 第二个「进程」：复用同一库文件，不重新登记，直接点名
        try (ConfigurableApplicationContext second = startContext(dbFile)) {
            SoilProfileService service = second.getBean(SoilProfileService.class);
            SoilProfileEntity reloaded = service.get(profileName);

            assertEquals(profileName, reloaded.getName());
            assertEquals(42.0, reloaded.getCohesionKpa(), 1e-9);
            assertEquals(0.0, reloaded.getFrictionAngleDeg(), 1e-9);
            assertEquals(19.5, reloaded.getUnitWeightKnM3(), 1e-9);
            assertEquals("持久化测试用硬黏土", reloaded.getDescription());

            // 重启后同样能取出计算用的参数对象
            var soil = service.requireParameters(profileName);
            assertEquals(42.0, soil.cohesionKpa(), 1e-9);
        }
    }
}
