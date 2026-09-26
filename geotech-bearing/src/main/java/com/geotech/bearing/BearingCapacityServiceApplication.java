package com.geotech.bearing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 浅基础极限承载力核算服务入口。
 *
 * <p>服务以具名「土层参数档」为一等公民：参数档持久化在 PostgreSQL 中，
 * 核算时既可以点名取用已登记参数档，也可以临时给全套土层参数。</p>
 */
@SpringBootApplication
public class BearingCapacityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BearingCapacityServiceApplication.class, args);
    }
}
