# ---- 构建阶段：固定 JDK 17（Eclipse Temurin） ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先只拷 pom 做依赖缓存层，利用 BuildKit 层缓存加速重复构建
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- 运行阶段：固定 JRE 17，镜像瘦身 ----
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# 容器内以非 root 运行（temurin 基础镜像自带 app 组/用户）
COPY --from=build /build/target/bearing-capacity-service-1.0.0.jar app.jar

EXPOSE 8080

# 简单存活探测，依赖 actuator 健康端点
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
  CMD ["sh", "-c", "wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '\"status\":\"UP\"' || exit 1"]

USER app
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
