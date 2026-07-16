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
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.AgentClient;
import com.ruoyi.system.service.AgentClientService;

/**
 * PartnerStack 数据代理
 */
@RestController
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

    private final AgentClientService agentClientService;

    public PartnerStackController(AgentClientService agentClientService)
    {
        this.agentClientService = agentClientService;
    }

    @GetMapping("/customers")
    public AjaxResult customers(@RequestParam(required = false) Long minCreated,
            @RequestParam(required = false) Long maxCreated,
            @RequestParam(required = false, defaultValue = "100") Integer limit,
            @RequestParam(required = false) String startingAfter,
            @RequestParam(required = false) String endingBefore)
    {
        String customerKey = scopedCustomerKey(null);
        return this.proxyList("/customers", buildCommonParams(minCreated, maxCreated, limit, startingAfter, endingBefore), customerKey);
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
        String scopedCustomerKey = scopedCustomerKey(customerKey);
        putIfPresent(params, "customer_key", scopedCustomerKey);
        return this.proxyList("/actions", params, scopedCustomerKey);
    }

    @GetMapping("/transactions")
    public AjaxResult transactions(@RequestParam(required = false) Long minCreated,
            @RequestParam(required = false) Long maxCreated,
            @RequestParam(required = false, defaultValue = "100") Integer limit,
            @RequestParam(required = false) String startingAfter,
            @RequestParam(required = false) String endingBefore)
    {
        String customerKey = scopedCustomerKey(null);
        return this.proxyList("/transactions", buildCommonParams(minCreated, maxCreated, limit, startingAfter, endingBefore), customerKey);
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
        String scopedCustomerKey = scopedCustomerKey(customerKey);
        putIfPresent(params, "customer_key", scopedCustomerKey);
        putIfPresent(params, "currency", currency);
        putIfPresent(params, "payment_status", paymentStatus);
        return this.proxyList("/rewards", params, scopedCustomerKey);
    }

    private AjaxResult proxyList(String path, Map<String, Object> params)
    {
        return proxyList(path, params, null);
    }

    private AjaxResult proxyList(String path, Map<String, Object> params, String customerKey)
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
                filterByCustomer(body, customerKey);
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

    private String scopedCustomerKey(String requestedCustomerKey)
    {
        if (!SecurityUtils.hasRole("agent"))
        {
            return requestedCustomerKey;
        }
        AgentClient agent = agentClientService.selectAgentBySysUserId(getUserId());
        if (agent == null || !StringUtils.hasText(agent.getPartnerCustomerKey()))
        {
            throw new ServiceException("当前代理未绑定 PartnerStack 客户Key");
        }
        return agent.getPartnerCustomerKey();
    }

    private void filterByCustomer(JSONObject body, String customerKey)
    {
        if (!StringUtils.hasText(customerKey) || body == null)
        {
            return;
        }
        filterContainer(body, customerKey);
    }

    private void filterContainer(JSONObject container, String customerKey)
    {
        for (String field : new String[] { "data", "items", "results" })
        {
            Object value = container.get(field);
            if (value instanceof JSONArray items)
            {
                items.removeIf(item -> !customerKey.equals(extractCustomerKey(item)));
            }
            else if (value instanceof JSONObject nested)
            {
                filterContainer(nested, customerKey);
            }
        }
    }

    private String extractCustomerKey(Object item)
    {
        if (!(item instanceof JSONObject object))
        {
            return null;
        }
        JSONObject customer = object.getJSONObject("customer");
        if (customer != null && StringUtils.hasText(customer.getString("key")))
        {
            return customer.getString("key");
        }
        if (StringUtils.hasText(object.getString("customer_key")))
        {
            return object.getString("customer_key");
        }
        return object.getString("key");
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
