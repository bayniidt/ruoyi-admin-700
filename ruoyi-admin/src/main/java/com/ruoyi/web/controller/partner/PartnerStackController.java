package com.ruoyi.web.controller.partner;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;

/**
 * PartnerStack 数据代理
 */
@RestController
@Anonymous
@RequestMapping("/partnerstack")
public class PartnerStackController extends BaseController
{
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${partnerstack.base-url:https://api.partnerstack.com/api/v2}")
    private String baseUrl;

    @Value("${partnerstack.token:}")
    private String token;

    @GetMapping("/customers")
    public AjaxResult customers(@RequestParam(required = false) Long minCreated,
            @RequestParam(required = false) Long maxCreated,
            @RequestParam(required = false, defaultValue = "100") Integer limit,
            @RequestParam(required = false) String startingAfter,
            @RequestParam(required = false) String endingBefore)
    {
        return this.proxyList("/customers", buildCommonParams(minCreated, maxCreated, limit, startingAfter, endingBefore));
    }

    @GetMapping("/actions")
    public AjaxResult actions(@RequestParam(required = false) Long minCreated,
            @RequestParam(required = false) Long maxCreated,
            @RequestParam(required = false) String customerKey,
            @RequestParam(required = false, defaultValue = "100") Integer limit,
            @RequestParam(required = false) String startingAfter,
            @RequestParam(required = false) String endingBefore)
    {
        Map<String, Object> params = buildCommonParams(minCreated, maxCreated, limit, startingAfter, endingBefore);
        putIfPresent(params, "customer_key", customerKey);
        return this.proxyList("/actions", params);
    }

    @GetMapping("/transactions")
    public AjaxResult transactions(@RequestParam(required = false) Long minCreated,
            @RequestParam(required = false) Long maxCreated,
            @RequestParam(required = false, defaultValue = "100") Integer limit,
            @RequestParam(required = false) String startingAfter,
            @RequestParam(required = false) String endingBefore)
    {
        return this.proxyList("/transactions", buildCommonParams(minCreated, maxCreated, limit, startingAfter, endingBefore));
    }

    @GetMapping("/rewards")
    public AjaxResult rewards(@RequestParam(required = false) Long minCreated,
            @RequestParam(required = false) Long maxCreated,
            @RequestParam(required = false) String customerKey,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false, defaultValue = "100") Integer limit,
            @RequestParam(required = false) String startingAfter,
            @RequestParam(required = false) String endingBefore)
    {
        Map<String, Object> params = buildCommonParams(minCreated, maxCreated, limit, startingAfter, endingBefore);
        putIfPresent(params, "customer_key", customerKey);
        putIfPresent(params, "currency", currency);
        putIfPresent(params, "payment_status", paymentStatus);
        return this.proxyList("/rewards", params);
    }

    private AjaxResult proxyList(String path, Map<String, Object> params)
    {
        if (!StringUtils.hasText(token))
        {
            return error("PartnerStack token 未配置");
        }

        String query = buildQuery(params);
        String url = baseUrl + path + (query.isEmpty() ? "" : "?" + query);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .build();
        try
        {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JSONObject body = JSON.parseObject(response.body());
            if (response.statusCode() >= 200 && response.statusCode() < 300)
            {
                return success(body);
            }
            return AjaxResult.error(response.statusCode(), body != null && body.containsKey("message") ? body.getString("message") : "PartnerStack 请求失败")
                    .put("data", body);
        }
        catch (IOException | InterruptedException e)
        {
            if (e instanceof InterruptedException)
            {
                Thread.currentThread().interrupt();
            }
            logger.error("PartnerStack request failed: {}", url, e);
            return error("PartnerStack 接口调用失败: " + e.getMessage());
        }
    }

    private Map<String, Object> buildCommonParams(Long minCreated, Long maxCreated, Integer limit, String startingAfter, String endingBefore)
    {
        Map<String, Object> params = new LinkedHashMap<>();
        putIfPresent(params, "min_created", minCreated);
        putIfPresent(params, "max_created", maxCreated);
        putIfPresent(params, "limit", limit);
        putIfPresent(params, "starting_after", startingAfter);
        putIfPresent(params, "ending_before", endingBefore);
        return params;
    }

    private void putIfPresent(Map<String, Object> params, String key, Object value)
    {
        if (value == null)
        {
            return;
        }
        if (value instanceof String stringValue)
        {
            if (!StringUtils.hasText(stringValue))
            {
                return;
            }
        }
        params.put(key, value);
    }

    private String buildQuery(Map<String, Object> params)
    {
        StringJoiner joiner = new StringJoiner("&");
        params.forEach((key, value) -> joiner.add(encode(key) + "=" + encode(String.valueOf(value))));
        return joiner.toString();
    }

    private String encode(String value)
    {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
