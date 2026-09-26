package com.geotech.bearing.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 端到端 HTTP/JSON 集成测试：参数档登记/查询/列出、核算、因子查询、宽度扫描、
 * 内置示范档、各类非法输入的结构化错误，以及多档登记 + 多次核算并发互不污染。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BearingApiIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    private final ObjectMapper json = new ObjectMapper();

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private Map<String, Object> inlineSoil(double c, double phi, double gamma) {
        Map<String, Object> m = new HashMap<>();
        m.put("cohesionKpa", c);
        m.put("frictionAngleDeg", phi);
        m.put("unitWeightKnM3", gamma);
        return m;
    }

    // ---------- 因子查询 ----------

    @Test
    @DisplayName("GET /factors?φ=30：返回接近文献表值的三因子")
    void factorsAtThirty() throws Exception {
        ResponseEntity<String> resp =
                rest.getForEntity(url("/api/bearings/factors?frictionAngleDeg=30"), String.class);
        assertEquals(HttpStatus.OK.value(), resp.getStatusCode().value());
        JsonNode node = json.readTree(resp.getBody());
        assertEquals(30.0, node.get("frictionAngleDeg").asDouble(), 1e-9);
        assertEquals(18.40, node.get("nq").asDouble(), 0.02);
        assertEquals(30.14, node.get("nc").asDouble(), 0.05);
        assertEquals(22.40, node.get("ngamma").asDouble(), 0.1);
    }

    @Test
    @DisplayName("GET /factors?φ=0：黏土极限 Nc=5.14、Nq=1、Nγ=0")
    void factorsAtClayLimit() throws Exception {
        ResponseEntity<String> resp =
                rest.getForEntity(url("/api/bearings/factors?frictionAngleDeg=0"), String.class);
        assertEquals(HttpStatus.OK.value(), resp.getStatusCode().value());
        JsonNode node = json.readTree(resp.getBody());
        assertEquals(5.14, node.get("nc").asDouble(), 1e-9);
        assertEquals(1.0, node.get("nq").asDouble(), 1e-9);
        assertEquals(0.0, node.get("ngamma").asDouble(), 1e-9);
    }

    // ---------- 非法输入：结构化错误且在算因子之前 ----------

    @Test
    @DisplayName("φ=90 必须被拦：400 + FRICTION_ANGLE_OUT_OF_RANGE（不让 tan 发散）")
    void angleNinetyRejectedOverHttp() {
        ResponseEntity<String> resp =
                rest.getForEntity(url("/api/bearings/factors?frictionAngleDeg=90"), String.class);
        assertEquals(HttpStatus.BAD_REQUEST.value(), resp.getStatusCode().value());
        assertReason(resp, "FRICTION_ANGLE_OUT_OF_RANGE");
    }

    @Test
    @DisplayName("临时参数核算：宽度非正 / 重度非正 / 埋深为负 / φ 为负 全部 400 且带原因")
    void invalidInlineCalculationsRejected() throws Exception {
        assertCalculateError(inlineCalcBody(10, 20, 18, 0, 1), "WIDTH_NOT_POSITIVE");
        assertCalculateError(inlineCalcBody(10, 20, 18, -2, 1), "WIDTH_NOT_POSITIVE");
        assertCalculateError(inlineCalcBody(10, 20, 0, 2, 1), "UNIT_WEIGHT_NOT_POSITIVE");
        assertCalculateError(inlineCalcBody(10, 20, 18, 2, -1), "DEPTH_NEGATIVE");
        assertCalculateError(inlineCalcBody(10, -5, 18, 2, 1), "FRICTION_ANGLE_NEGATIVE");
    }

    private Map<String, Object> inlineCalcBody(double c, double phi, double gamma,
                                               double width, double depth) {
        Map<String, Object> body = new HashMap<>();
        body.put("soil", inlineSoil(c, phi, gamma));
        body.put("widthM", width);
        body.put("depthM", depth);
        return body;
    }

    private void assertCalculateError(Map<String, Object> body, String reason) {
        ResponseEntity<String> resp =
                rest.postForEntity(url("/api/bearings/calculate"), body, String.class);
        assertEquals(HttpStatus.BAD_REQUEST.value(), resp.getStatusCode().value(),
                "非法输入应返回 400：" + body);
        assertReason(resp, reason);
    }

    private void assertReason(ResponseEntity<String> resp, String reason) {
        try {
            JsonNode node = json.readTree(resp.getBody());
            assertEquals(reason, node.get("error").asText(),
                    "错误原因码应为 " + reason + "，实际响应：" + resp.getBody());
        } catch (Exception e) {
            throw new AssertionError("响应不是合法 JSON：" + resp.getBody(), e);
        }
    }

    // ---------- 参数档：登记/列出/点名/未登记 ----------

    @Test
    @DisplayName("登记 -> 列出包含 -> 按名查询 -> 点名核算 全链路")
    void registerListGetAndCalculateByName() throws Exception {
        String name = "clay-" + UUID.randomUUID();
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("cohesionKpa", 30);
        body.put("frictionAngleDeg", 0);
        body.put("unitWeightKnM3", 18);
        body.put("description", "测试用软黏土");

        ResponseEntity<String> created = rest.postForEntity(url("/api/profiles"), body, String.class);
        assertEquals(HttpStatus.CREATED.value(), created.getStatusCode().value());

        ResponseEntity<String> got = rest.getForEntity(url("/api/profiles/" + name), String.class);
        assertEquals(HttpStatus.OK.value(), got.getStatusCode().value());
        assertEquals(30.0, json.readTree(got.getBody()).get("cohesionKpa").asDouble(), 1e-9);

        ResponseEntity<String> listed = rest.getForEntity(url("/api/profiles"), String.class);
        assertTrue(listed.getBody().contains(name), "列出结果应包含新登记档");

        // 点名核算：φ=0 黏土极限，qu = 30·5.14 + 18·1.5
        Map<String, Object> calcBody = new HashMap<>();
        calcBody.put("profileName", name);
        calcBody.put("widthM", 2.0);
        calcBody.put("depthM", 1.5);
        ResponseEntity<String> calc =
                rest.postForEntity(url("/api/bearings/calculate"), calcBody, String.class);
        assertEquals(HttpStatus.OK.value(), calc.getStatusCode().value());
        JsonNode result = json.readTree(calc.getBody());
        assertEquals("PROFILE", result.get("source").asText());
        assertEquals(30 * 5.14 + 18 * 1.5,
                result.get("terms").get("quKpa").asDouble(), 1e-7);
        assertEquals(0.0, result.get("terms").get("weightTermKpa").asDouble(), 1e-9,
                "黏土极限自重项为 0");
    }

    @Test
    @DisplayName("点名未登记档 -> 404 PROFILE_NOT_FOUND，不拿默认值凑结果")
    void unknownProfileIsNotFound() {
        Map<String, Object> calcBody = new HashMap<>();
        calcBody.put("profileName", "no-such-profile-" + UUID.randomUUID());
        calcBody.put("widthM", 2.0);
        calcBody.put("depthM", 1.0);
        ResponseEntity<String> resp =
                rest.postForEntity(url("/api/bearings/calculate"), calcBody, String.class);
        assertEquals(HttpStatus.NOT_FOUND.value(), resp.getStatusCode().value());
        assertReason(resp, "PROFILE_NOT_FOUND");
    }

    @Test
    @DisplayName("重复登记同名档 -> 409 PROFILE_ALREADY_EXISTS")
    void duplicateProfileConflicts() {
        String name = "dup-" + UUID.randomUUID();
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("cohesionKpa", 0);
        body.put("frictionAngleDeg", 30);
        body.put("unitWeightKnM3", 19);

        assertEquals(HttpStatus.CREATED.value(),
                rest.postForEntity(url("/api/profiles"), body, String.class)
                        .getStatusCode().value());
        ResponseEntity<String> again =
                rest.postForEntity(url("/api/profiles"), body, String.class);
        assertEquals(HttpStatus.CONFLICT.value(), again.getStatusCode().value());
        assertReason(again, "PROFILE_ALREADY_EXISTS");
    }

    // ---------- 内置示范档 + 宽度扫描 ----------

    @Test
    @DisplayName("内置中密砂示范档：因子接近表值，扫描时 qu 随宽度严格增大")
    void demoSandProfileAndScanTrend() throws Exception {
        // 内置档应已随启动登记
        ResponseEntity<String> demo =
                rest.getForEntity(url("/api/profiles/medium-dense-sand"), String.class);
        assertEquals(HttpStatus.OK.value(), demo.getStatusCode().value());

        Map<String, Object> scanBody = new HashMap<>();
        scanBody.put("profileName", "medium-dense-sand");
        scanBody.put("depthM", 1.0);
        scanBody.put("shape", "STRIP");
        scanBody.put("minWidthM", 1.0);
        scanBody.put("maxWidthM", 4.0);
        scanBody.put("stepM", 1.0);

        ResponseEntity<String> scan =
                rest.postForEntity(url("/api/bearings/scan"), scanBody, String.class);
        assertEquals(HttpStatus.OK.value(), scan.getStatusCode().value());
        JsonNode root = json.readTree(scan.getBody());
        assertEquals(4, root.get("count").asInt());

        double prevQu = -1;
        double firstNq = root.get("points").get(0).get("factors").get("nq").asDouble();
        assertEquals(18.40, firstNq, 0.02);
        for (JsonNode point : root.get("points")) {
            double qu = point.get("terms").get("quKpa").asDouble();
            assertTrue(qu > prevQu, "c=0 砂土 qu 应随宽度严格增大：" + prevQu + " -> " + qu);
            prevQu = qu;
        }

        // 宽度加倍，qu 增量等于自重项（手工核对联动）
        double quB1 = root.get("points").get(0).get("terms").get("quKpa").asDouble();
        double quB2 = root.get("points").get(1).get("terms").get("quKpa").asDouble();
        double weightB1 = root.get("points").get(0).get("terms").get("weightTermKpa").asDouble();
        // B 从 1->2，自重项翻倍，差额即 weightTerm(B=1)
        assertEquals(weightB1, quB2 - quB1, 1e-7);
    }

    @Test
    @DisplayName("方形形状修正通过 HTTP 生效：三项系数回显且 qu 不同于条形")
    void squareShapeOverHttp() throws Exception {
        Map<String, Object> body = inlineCalcBody(10, 25, 19, 3, 1.5);
        body.put("shape", "SQUARE");
        ResponseEntity<String> square =
                rest.postForEntity(url("/api/bearings/calculate"), body, String.class);
        assertEquals(HttpStatus.OK.value(), square.getStatusCode().value());
        JsonNode node = json.readTree(square.getBody());
        assertEquals(1.3, node.get("shapeFactors").get("sc").asDouble(), 1e-9);
        assertEquals(0.8, node.get("shapeFactors").get("sGamma").asDouble(), 1e-9);

        body.put("shape", "STRIP");
        ResponseEntity<String> strip =
                rest.postForEntity(url("/api/bearings/calculate"), body, String.class);
        double stripQu = json.readTree(strip.getBody()).get("terms").get("quKpa").asDouble();
        double squareQu = node.get("terms").get("quKpa").asDouble();
        assertTrue(Math.abs(stripQu - squareQu) > 1e-6, "方形与条形 qu 应不同");
    }

    // ---------- 并发：多档登记 + 多次核算互不写串 ----------

    @Test
    @DisplayName("并发：N 个线程各自登记独立参数档并核算，结果互不污染")
    void concurrentRegistrationsAndCalculationsAreIsolated() throws Exception {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ConcurrentHashMap<String, Double> expectedQu = new ConcurrentHashMap<>();
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    start.await();
                    String name = "conc-" + UUID.randomUUID() + "-" + idx;
                    double c = 5 * idx;
                    double phi = 5 * idx; // 0,5,...,35
                    double gamma = 18 + idx;

                    Map<String, Object> reg = new HashMap<>();
                    reg.put("name", name);
                    reg.put("cohesionKpa", c);
                    reg.put("frictionAngleDeg", phi);
                    reg.put("unitWeightKnM3", gamma);
                    assertEquals(HttpStatus.CREATED.value(),
                            rest.postForEntity(url("/api/profiles"), reg, String.class)
                                    .getStatusCode().value());

                    // 同名多次核算，结果必须稳定一致
                    List<Double> qus = new ArrayList<>();
                    for (int k = 0; k < 5; k++) {
                        Map<String, Object> calc = new HashMap<>();
                        calc.put("profileName", name);
                        calc.put("widthM", 2.0);
                        calc.put("depthM", 1.0);
                        ResponseEntity<String> r =
                                rest.postForEntity(url("/api/bearings/calculate"), calc, String.class);
                        if (r.getStatusCode() != HttpStatus.OK) {
                            failures.incrementAndGet();
                            return;
                        }
                        qus.add(json.readTree(r.getBody()).get("terms").get("quKpa").asDouble());
                    }
                    double first = qus.get(0);
                    for (double q : qus) {
                        if (Math.abs(q - first) > 1e-7) {
                            failures.incrementAndGet();
                            return;
                        }
                    }
                    expectedQu.put(name, first);
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(60, TimeUnit.SECONDS), "并发任务超时");
        pool.shutdown();

        assertEquals(0, failures.get(), "并发核算不应有失败或结果漂移");
        assertEquals(threads, expectedQu.size());
        // 不同参数档应得到不同 qu（φ 不同），证明没有写串
        long distinct = expectedQu.values().stream().distinct().count();
        assertEquals(threads, distinct, "不同参数档的 qu 必须互不相同，结果未被污染");
    }

    @Test
    @DisplayName("并发登记同一档名：恰好一个成功，其余全部 409")
    void concurrentDuplicateRegistrationSingleWinner() throws Exception {
        int threads = 6;
        String name = "race-" + UUID.randomUUID();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger created = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        List<String> failures = java.util.Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    Map<String, Object> reg = new HashMap<>();
                    reg.put("name", name);
                    reg.put("cohesionKpa", 0);
                    reg.put("frictionAngleDeg", 30);
                    reg.put("unitWeightKnM3", 19);
                    int code =
                            rest.postForEntity(url("/api/profiles"), reg, String.class)
                                    .getStatusCode().value();
                    if (code == HttpStatus.CREATED.value()) {
                        created.incrementAndGet();
                    } else if (code == HttpStatus.CONFLICT.value()) {
                        conflicts.incrementAndGet();
                    } else {
                        failures.add("线程得到非预期状态码：" + code);
                    }
                } catch (Exception ex) {
                    // 不允许出现 201/409 以外的结果（如 500）
                    failures.add("线程得到非预期异常：" + ex);
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS));
        pool.shutdown();

        assertTrue(failures.isEmpty(), "不应有非预期结果：" + failures);
        assertEquals(1, created.get(), "同名并发登记只能有一笔成功");
        assertEquals(threads - 1, conflicts.get(), "其余必须冲突 409");
    }
}
