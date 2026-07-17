package com.ruoyi.web.controller.partner;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.service.ISysUserService;

/**
 * PartnerStack 数据代理
 */
@RestController
@RequestMapping("/partnerstack")
public class PartnerStackController extends BaseController
{
    private static final String FIXED_PARTNERSTACK_KEY = "fay2dGIxZKSls3K5USkVs0eGZ7N10mkuMytLrMbzDObrFglXoZenMfw8TqAGdryt";
    private static final int PARTNERSTACK_PAGE_SIZE = 250;
    private static final int MAX_PAGES = 100;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${partnerstack.base-url:https://api.partnerstack.com/api/v2}")
    private String baseUrl;

    @Value("${partnerstack.token:}")
    private String platformToken;

    private final ISysUserService userService;

    public PartnerStackController(ISysUserService userService)
    {
        this.userService = userService;
    }

    /**
     * 首页汇总。PartnerStack 金额字段单位为美分，此接口统一转换为美元。
     */
    @GetMapping("/dashboard")
    public AjaxResult dashboard(@RequestParam(required = false) Long minCreated,
            @RequestParam(required = false) Long maxCreated,
            @RequestParam(required = false) String subId,
            @RequestParam(required = false) String transactionId)
    {
        if (minCreated != null && maxCreated != null && minCreated > maxCreated)
        {
            return error("开始时间不能晚于结束时间");
        }

        PartnerAccess access = scopedPartnerAccess();
        try
        {
            Scope scope = access.scope();
            Map<String, Object> customerParams = new LinkedHashMap<>();
            JSONArray customerSource;
            if (scope.requiresCustomerResolution())
            {
                customerSource = fetchAllItems("/customers", customerParams, access.token());
                scope = resolveScope(scope, customerSource);
            }
            else
            {
                customerParams = buildCommonParams(minCreated, maxCreated, PARTNERSTACK_PAGE_SIZE, null, null);
                customerSource = fetchAllItems("/customers", customerParams, access.token());
            }

            JSONArray customers = filterItems(customerSource, scope, subId, minCreated, maxCreated,
                    access.fallbackSubId());
            Map<String, Object> eventParams = buildCommonParams(minCreated, maxCreated, PARTNERSTACK_PAGE_SIZE, null, null);
            putIfPresent(eventParams, "customer_key", scope.queryCustomerKey());
            JSONArray actions = filterItems(fetchAllItems("/actions", eventParams, access.token()), scope, subId,
                    minCreated, maxCreated, access.fallbackSubId());
            JSONArray transactions = filterItems(fetchAllItems("/transactions",
                    buildCommonParams(minCreated, maxCreated, PARTNERSTACK_PAGE_SIZE, null, null), access.token()), scope, subId,
                    minCreated, maxCreated, access.fallbackSubId());
            JSONArray rewards = filterItems(fetchAllItems("/rewards", eventParams, access.token()), scope, subId,
                    minCreated, maxCreated, access.fallbackSubId());

            if (StringUtils.hasText(transactionId))
            {
                customers.clear();
                actions.clear();
                transactions.removeIf(item -> !containsIgnoreCase(((JSONObject) item).getString("key"), transactionId));
                rewards.removeIf(item -> !containsIgnoreCase(((JSONObject) item).getJSONObject("source") == null
                        ? null : ((JSONObject) item).getJSONObject("source").getString("key"), transactionId));
            }

            return success(buildDashboard(access.displayKey(), access.fallbackSubId(), customers, actions,
                    transactions, rewards));
        }
        catch (PartnerStackApiException e)
        {
            logger.error("PartnerStack dashboard request failed: {}", e.getMessage());
            return AjaxResult.error(e.getStatus(), e.getMessage());
        }
    }

    @GetMapping("/customers")
    public AjaxResult customers(@RequestParam(required = false) Long minCreated,
            @RequestParam(required = false) Long maxCreated,
            @RequestParam(required = false, defaultValue = "100") Integer limit,
            @RequestParam(required = false) String startingAfter,
            @RequestParam(required = false) String endingBefore)
    {
        PartnerAccess access = scopedPartnerAccess();
        AjaxResult result = this.proxyList("/customers",
                buildCommonParams(minCreated, maxCreated, limit, startingAfter, endingBefore), access);
        if (result.isSuccess())
        {
            attachPromoLinks(result);
        }
        return result;
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
        PartnerAccess access = scopedPartnerAccess();
        String scopedCustomerKey = access.scope().queryCustomerKey();
        if (StringUtils.hasText(scopedCustomerKey))
        {
            putIfPresent(params, "customer_key", scopedCustomerKey);
        }
        return this.proxyList("/actions", params, access);
    }

    @GetMapping("/transactions")
    public AjaxResult transactions(@RequestParam(required = false) Long minCreated,
            @RequestParam(required = false) Long maxCreated,
            @RequestParam(required = false) String subId,
            @RequestParam(required = false, defaultValue = "100") Integer limit,
            @RequestParam(required = false) String startingAfter,
            @RequestParam(required = false) String endingBefore)
    {
        PartnerAccess access = scopedPartnerAccess();
        AjaxResult result = this.proxyList("/transactions",
                buildCommonParams(minCreated, maxCreated, limit, startingAfter, endingBefore), access);
        if (result.isSuccess() && StringUtils.hasText(subId))
        {
            filterListResultBySubId(result, subId, access.fallbackSubId());
        }
        return result;
    }

    @GetMapping("/transaction-details")
    public AjaxResult transactionDetails(@RequestParam("customerKey") String customerKey,
            @RequestParam(value = "subId", required = false) String subId)
    {
        if (!StringUtils.hasText(customerKey))
        {
            return error("广告户ID不能为空");
        }
        PartnerAccess access = scopedPartnerAccess();
        try
        {
            Scope scope = access.scope();
            if (scope.requiresCustomerResolution())
            {
                JSONArray customerSource = fetchAllItems("/customers", new LinkedHashMap<>(), access.token());
                scope = resolveScope(scope, customerSource);
            }
            JSONArray actions = filterItems(fetchAllItems("/actions",
                    buildCommonParams(null, null, PARTNERSTACK_PAGE_SIZE, null, null), access.token()), scope, subId,
                    null, null, access.fallbackSubId());
            JSONArray transactions = filterItems(fetchAllItems("/transactions",
                    buildCommonParams(null, null, PARTNERSTACK_PAGE_SIZE, null, null), access.token()), scope, subId,
                    null, null, access.fallbackSubId());
            List<JSONObject> rows = new ArrayList<>();
            rows.add(buildTimelineRow("customer_created", customerKey, BigDecimal.ZERO, null));
            for (Object item : actions)
            {
                if (!(item instanceof JSONObject action))
                {
                    continue;
                }
                if (!customerKey.equals(extractCustomerKey(action)))
                {
                    continue;
                }
                rows.add(buildTimelineRow("action_created", action.getString("key"), BigDecimal.ZERO, action));
            }
            for (Object item : transactions)
            {
                if (!(item instanceof JSONObject transaction))
                {
                    continue;
                }
                if (!customerKey.equals(extractCustomerKey(transaction)))
                {
                    continue;
                }
                BigDecimal amount = cents(transaction.containsKey("amount_usd")
                        ? transaction.get("amount_usd") : transaction.get("amount"));
                rows.add(buildTimelineRow("transaction_created", transaction.getString("key"), amount, transaction));
            }
            rows.sort(Comparator.comparing((JSONObject row) -> row.getString("eventTime"),
                    Comparator.nullsLast(Comparator.reverseOrder())));
            BigDecimal totalAmount = rows.stream()
                    .map(row -> row.getBigDecimal("amountSUM"))
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return AjaxResult.success(new JSONObject()
                    .fluentPut("total", rows.size())
                    .fluentPut("totalAmountSUM", money(totalAmount))
                    .fluentPut("rows", rows));
        }
        catch (PartnerStackApiException e)
        {
            logger.error("PartnerStack transaction details request failed: {}", e.getMessage());
            return AjaxResult.error(e.getStatus(), e.getMessage());
        }
    }

    @GetMapping("/ad-accounts")
    public AjaxResult adAccounts(@RequestParam(required = false) Long minCreated,
            @RequestParam(required = false) Long maxCreated,
            @RequestParam(required = false) String subId,
            @RequestParam(required = false) String customerKey,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "0") BigDecimal minAmountSUM,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize)
    {
        if (minCreated != null && maxCreated != null && minCreated > maxCreated)
        {
            return error("开始时间不能晚于结束时间");
        }

        PartnerAccess access = scopedPartnerAccess();
        try
        {
            Scope scope = access.scope();
            int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
            int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
            JSONArray customerSource;
            if (scope.requiresCustomerResolution())
            {
                customerSource = fetchAllItems("/customers", new LinkedHashMap<>(), access.token());
                scope = resolveScope(scope, customerSource);
            }
            else
            {
                customerSource = fetchAllItems("/customers",
                        buildCommonParams(minCreated, maxCreated, PARTNERSTACK_PAGE_SIZE, null, null), access.token());
            }
            JSONArray customers = filterItems(customerSource, scope, subId, minCreated, maxCreated,
                    access.fallbackSubId());
            List<JSONObject> rows = new ArrayList<>();
            for (Object item : customers)
            {
                if (!(item instanceof JSONObject customer))
                {
                    continue;
                }
                if (!matchesAdAccountId(customer, customerKey))
                {
                    continue;
                }
                boolean hasPaid = customer.getBooleanValue("has_paid");
                if (StringUtils.hasText(status)
                        && (("paid".equalsIgnoreCase(status) && !hasPaid)
                                || ("registered".equalsIgnoreCase(status) && hasPaid)))
                {
                    continue;
                }
                String currentCustomerKey = customer.getString("key");
                BigDecimal amountSum = decimalFromCents(customer.getBigDecimal("amount_sum"));
                if (amountSum.compareTo(minAmountSUM == null ? BigDecimal.ZERO : minAmountSUM) < 0)
                {
                    continue;
                }
                JSONObject row = new JSONObject();
                row.put("customerKey", currentCustomerKey);
                row.put("contact", StringUtils.hasText(customer.getString("customer_key"))
                        ? customer.getString("customer_key") : currentCustomerKey);
                row.put("subId", firstSubId(customer) != null ? firstSubId(customer) : access.fallbackSubId());
                row.put("countryIso", extractCountry(customer));
                row.put("hasPaid", hasPaid ? 1 : 0);
                row.put("sourceType", "推荐链接");
                row.put("totalRevenue", decimalFromCents(customer.getBigDecimal("total_revenue")));
                row.put("createdDate", formatDateOnly(customer.getLong("created_at")));
                row.put("updatedAt", formatIsoDateTime(customer.getLong("updated_at")));
                row.put("createdAt", formatIsoDateTime(customer.getLong("created_at")));
                row.put("amountSUM", amountSum.setScale(2, RoundingMode.HALF_UP));
                rows.add(row);
            }
            rows.sort(Comparator.comparing((JSONObject row) -> row.getString("updatedAt"),
                    Comparator.nullsLast(Comparator.reverseOrder())));
            List<JSONObject> pagedRows = pageRows(rows, safePageNum, safePageSize);
            return AjaxResult.success(new JSONObject()
                    .fluentPut("total", rows.size())
                    .fluentPut("rows", pagedRows));
        }
        catch (PartnerStackApiException e)
        {
            logger.error("PartnerStack ad accounts request failed: {}", e.getMessage());
            return AjaxResult.error(e.getStatus(), e.getMessage());
        }
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
        PartnerAccess access = scopedPartnerAccess();
        String scopedCustomerKey = access.scope().queryCustomerKey();
        if (StringUtils.hasText(scopedCustomerKey))
        {
            putIfPresent(params, "customer_key", scopedCustomerKey);
        }
        putIfPresent(params, "currency", currency);
        putIfPresent(params, "payment_status", paymentStatus);
        return this.proxyList("/rewards", params, access);
    }

    private AjaxResult proxyList(String path, Map<String, Object> params, PartnerAccess access)
    {
        if (!StringUtils.hasText(access.token()))
        {
            return error("PartnerStack token 未配置");
        }

        String query = buildQuery(params);
        String url = baseUrl + path + (query.isEmpty() ? "" : "?" + query);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + access.token())
                .header("Accept", "application/json")
                .GET()
                .build();
        try
        {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JSONObject body = JSON.parseObject(response.body());
            if (response.statusCode() >= 200 && response.statusCode() < 300)
            {
                filterByScope(body, access.scope());
                return success(body);
            }
            return AjaxResult.error(502, body != null && body.containsKey("message") ? body.getString("message") : "PartnerStack 请求失败")
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
        catch (RuntimeException e)
        {
            logger.error("PartnerStack returned an invalid response: {}", url, e);
            return AjaxResult.error(502, "PartnerStack 响应格式错误");
        }
    }

    private PartnerAccess scopedPartnerAccess()
    {
        SysUser user = userService.selectUserById(getUserId());
        if (user == null)
        {
            throw new ServiceException("当前用户不存在");
        }
        String userKey = FIXED_PARTNERSTACK_KEY;
        if (looksLikeAccessToken(userKey))
        {
            return new PartnerAccess(userKey, Scope.all(), maskSecret(userKey), user.getUserName());
        }
        if (!StringUtils.hasText(platformToken))
        {
            throw new ServiceException("当前账号绑定的是 PartnerStack 数据Key，但系统未配置 PARTNERSTACK_TOKEN");
        }
        return new PartnerAccess(platformToken.trim(), Scope.from(userKey), userKey, user.getUserName());
    }

    private boolean looksLikeAccessToken(String value)
    {
        return StringUtils.hasText(value) && value.length() >= 32
                && !value.startsWith("part_") && !value.startsWith("cus_");
    }

    private String maskSecret(String value)
    {
        if (!StringUtils.hasText(value) || value.length() <= 12)
        {
            return "已绑定";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    private void filterByScope(JSONObject body, Scope scope)
    {
        if (body == null || scope.matchesAll())
        {
            return;
        }
        filterContainer(body, scope);
    }

    private void filterContainer(JSONObject container, Scope scope)
    {
        for (String field : new String[] { "data", "items", "results" })
        {
            Object value = container.get(field);
            if (value instanceof JSONArray items)
            {
                items.removeIf(item -> !(item instanceof JSONObject object) || !matchesScope(object, scope));
            }
            else if (value instanceof JSONObject nested)
            {
                filterContainer(nested, scope);
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
        return object.getString("key");
    }

    private void attachPromoLinks(AjaxResult result)
    {
        Object body = result.get(AjaxResult.DATA_TAG);
        if (!(body instanceof JSONObject container))
        {
            return;
        }
        JSONObject data = container.getJSONObject("data");
        if (data == null)
        {
            return;
        }
        JSONArray items = data.getJSONArray("items");
        if (items == null)
        {
            return;
        }
        for (Object item : items)
        {
            if (item instanceof JSONObject customer)
            {
                customer.put("promoLink", extractPromoLink(customer));
            }
        }
    }

    private void filterListResultBySubId(AjaxResult result, String subId, String fallbackSubId)
    {
        Object body = result.get(AjaxResult.DATA_TAG);
        if (!(body instanceof JSONObject container))
        {
            return;
        }
        JSONObject data = container.getJSONObject("data");
        if (data == null)
        {
            return;
        }
        JSONArray items = data.getJSONArray("items");
        if (items != null)
        {
            items.removeIf(item -> !(item instanceof JSONObject object) || !matchesSubId(object, subId, fallbackSubId));
        }
    }

    private String extractPromoLink(JSONObject customer)
    {
        String[] directFields = { "promo_link", "promoLink", "partner_link", "partnerLink", "tracking_link",
                "trackingLink", "referral_link", "referralLink", "share_url", "shareUrl", "url" };
        for (String field : directFields)
        {
            String value = customer.getString(field);
            if (looksLikePromoLink(value))
            {
                return value;
            }
        }

        for (String nestedField : new String[] { "link", "links", "referral_link", "referralLink", "partner_link",
                "partnerLink", "promo_link", "promoLink" })
        {
            String value = extractPromoLinkValue(customer.get(nestedField));
            if (looksLikePromoLink(value))
            {
                return value;
            }
        }
        return null;
    }

    private String extractPromoLinkValue(Object value)
    {
        if (value instanceof String text)
        {
            return text;
        }
        if (value instanceof JSONObject object)
        {
            for (String field : new String[] { "url", "link", "value", "short_link", "shortLink", "tracking_link",
                    "trackingLink", "promo_link", "promoLink" })
            {
                String text = object.getString(field);
                if (StringUtils.hasText(text))
                {
                    return text;
                }
            }
        }
        if (value instanceof JSONArray array)
        {
            for (Object item : array)
            {
                String text = extractPromoLinkValue(item);
                if (StringUtils.hasText(text))
                {
                    return text;
                }
            }
        }
        return null;
    }

    private boolean looksLikePromoLink(String value)
    {
        return StringUtils.hasText(value)
                && (value.startsWith("http://") || value.startsWith("https://"));
    }

    private JSONArray fetchAllItems(String path, Map<String, Object> sourceParams, String accessToken)
    {
        if (!StringUtils.hasText(accessToken))
        {
            throw new PartnerStackApiException(500, "PartnerStack token 未配置");
        }

        JSONArray result = new JSONArray();
        String cursor = null;
        for (int page = 0; page < MAX_PAGES; page++)
        {
            Map<String, Object> params = new LinkedHashMap<>(sourceParams);
            params.remove("ending_before");
            params.put("limit", PARTNERSTACK_PAGE_SIZE);
            params.putIfAbsent("order_by", "-created_at");
            if (StringUtils.hasText(cursor))
            {
                params.put("starting_after", cursor);
            }

            JSONObject body = requestPartnerStack(path, params, accessToken);
            JSONObject data = body.getJSONObject("data");
            JSONArray items = data == null ? null : data.getJSONArray("items");
            if (items == null || items.isEmpty())
            {
                return result;
            }
            result.addAll(items);
            if (!data.getBooleanValue("has_more"))
            {
                return result;
            }
            JSONObject last = items.getJSONObject(items.size() - 1);
            String nextCursor = last == null ? null : last.getString("key");
            if (!StringUtils.hasText(nextCursor) || nextCursor.equals(cursor))
            {
                throw new PartnerStackApiException(502, "PartnerStack 分页游标异常");
            }
            cursor = nextCursor;
        }
        throw new PartnerStackApiException(502, "PartnerStack 返回数据超过单次汇总上限，请缩小日期范围");
    }

    private JSONObject requestPartnerStack(String path, Map<String, Object> params, String accessToken)
    {
        String query = buildQuery(params);
        String url = baseUrl + path + (query.isEmpty() ? "" : "?" + query);
        return executePartnerStackRequest(url, accessToken);
    }

    private JSONObject executePartnerStackRequest(String url, String accessToken)
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .build();
        try
        {
            HttpResponse<String> response = HTTP_CLIENT.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JSONObject body = JSON.parseObject(response.body());
            if (response.statusCode() >= 200 && response.statusCode() < 300 && body != null)
            {
                return body;
            }
            String message = body != null && StringUtils.hasText(body.getString("message"))
                    ? body.getString("message") : "PartnerStack 请求失败";
            throw new PartnerStackApiException(502,
                    "PartnerStack 请求失败（HTTP " + response.statusCode() + "）: " + message);
        }
        catch (IOException e)
        {
            throw new PartnerStackApiException(502, "PartnerStack 接口调用失败: " + e.getMessage());
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new PartnerStackApiException(502, "PartnerStack 接口调用被中断");
        }
        catch (RuntimeException e)
        {
            if (e instanceof PartnerStackApiException apiException)
            {
                throw apiException;
            }
            throw new PartnerStackApiException(502, "PartnerStack 响应格式错误");
        }
    }

    private Scope resolveScope(Scope unresolved, JSONArray customers)
    {
        Set<String> customerKeys = new HashSet<>();
        for (Object item : customers)
        {
            if (!(item instanceof JSONObject customer))
            {
                continue;
            }
            if (unresolved.rawKey().equals(customer.getString("customer_key"))
                    || unresolved.rawKey().equals(customer.getString("shared_id"))
                    || contains(customer.getJSONArray("sub_ids"), unresolved.rawKey()))
            {
                String customerKey = customer.getString("key");
                if (StringUtils.hasText(customerKey))
                {
                    customerKeys.add(customerKey);
                }
            }
        }
        return unresolved.withCustomerKeys(customerKeys);
    }

    private JSONArray filterItems(JSONArray source, Scope scope, String subId, Long minCreated, Long maxCreated,
            String fallbackSubId)
    {
        JSONArray result = new JSONArray();
        for (Object item : source)
        {
            if (item instanceof JSONObject object && matchesScope(object, scope)
                    && matchesSubId(object, subId, fallbackSubId)
                    && matchesCreatedAt(object, minCreated, maxCreated))
            {
                result.add(object);
            }
        }
        return result;
    }

    private boolean matchesCreatedAt(JSONObject item, Long minCreated, Long maxCreated)
    {
        if (minCreated == null && maxCreated == null)
        {
            return true;
        }
        long createdAt = item.getLongValue("created_at");
        if (createdAt <= 0)
        {
            return false;
        }
        return (minCreated == null || createdAt >= minCreated) && (maxCreated == null || createdAt <= maxCreated);
    }

    private boolean matchesScope(JSONObject item, Scope scope)
    {
        if (scope.matchesAll())
        {
            return true;
        }
        if (scope.isPartnership())
        {
            return scope.rawKey().equals(item.getString("partnership_key"));
        }
        String customerKey = extractCustomerKey(item);
        if (scope.customerKeys().contains(customerKey))
        {
            return true;
        }
        if (scope.requiresCustomerResolution())
        {
            return scope.rawKey().equals(item.getString("customer_key"))
                    || scope.rawKey().equals(item.getString("shared_id"))
                    || contains(item.getJSONArray("sub_ids"), scope.rawKey());
        }
        return false;
    }

    private boolean matchesSubId(JSONObject item, String subId, String fallbackSubId)
    {
        if (!StringUtils.hasText(subId))
        {
            return true;
        }
        JSONObject customer = item.getJSONObject("customer");
        JSONArray subIds = customer == null ? item.getJSONArray("sub_ids") : customer.getJSONArray("sub_ids");
        return contains(subIds, subId)
                || ((subIds == null || subIds.isEmpty()) && subId.equals(fallbackSubId));
    }

    private boolean contains(JSONArray values, String expected)
    {
        if (values == null || !StringUtils.hasText(expected))
        {
            return false;
        }
        return values.stream().anyMatch(value -> expected.equals(String.valueOf(value)));
    }

    private boolean containsIgnoreCase(String value, String query)
    {
        return StringUtils.hasText(value) && StringUtils.hasText(query)
                && value.toLowerCase().contains(query.trim().toLowerCase());
    }

    static boolean matchesAdAccountId(JSONObject customer, String query)
    {
        if (!StringUtils.hasText(query))
        {
            return true;
        }
        return containsTextIgnoreCase(customer.getString("key"), query)
                || containsTextIgnoreCase(customer.getString("customer_key"), query)
                || containsTextIgnoreCase(customer.getString("shared_id"), query);
    }

    static <T> List<T> pageRows(List<T> rows, int pageNum, int pageSize)
    {
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.max(pageSize, 1);
        int fromIndex = Math.min((safePageNum - 1) * safePageSize, rows.size());
        int toIndex = Math.min(fromIndex + safePageSize, rows.size());
        return rows.subList(fromIndex, toIndex);
    }

    private static boolean containsTextIgnoreCase(String value, String query)
    {
        return StringUtils.hasText(value) && StringUtils.hasText(query)
                && value.toLowerCase(java.util.Locale.ROOT)
                        .contains(query.trim().toLowerCase(java.util.Locale.ROOT));
    }

    private BigDecimal decimalFromCents(BigDecimal value)
    {
        if (value == null)
        {
            return BigDecimal.ZERO;
        }
        return value.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private String formatIsoDateTime(Long value)
    {
        if (value == null || value <= 0)
        {
            return null;
        }
        java.time.OffsetDateTime dateTime = java.time.Instant.ofEpochMilli(value)
                .atOffset(java.time.ZoneOffset.ofHours(8));
        return dateTime.toString();
    }

    private String formatDateTime(Long value)
    {
        if (value == null || value <= 0)
        {
            return null;
        }
        java.time.LocalDateTime dateTime = java.time.Instant.ofEpochMilli(value)
                .atZone(java.time.ZoneId.of("Asia/Shanghai"))
                .toLocalDateTime();
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String formatDateOnly(Long value)
    {
        if (value == null || value <= 0)
        {
            return null;
        }
        java.time.LocalDate date = java.time.Instant.ofEpochMilli(value)
                .atZone(java.time.ZoneId.of("Asia/Shanghai"))
                .toLocalDate();
        return date.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", java.util.Locale.ENGLISH));
    }

    private String extractCountry(JSONObject customer)
    {
        if (customer == null)
        {
            return "-";
        }
        String country = customer.getString("country_iso");
        if (StringUtils.hasText(country))
        {
            return country;
        }
        JSONObject fieldData = customer.getJSONObject("field_data");
        if (fieldData != null && StringUtils.hasText(fieldData.getString("country_iso")))
        {
            return fieldData.getString("country_iso");
        }
        return "-";
    }

    private JSONObject buildTimelineRow(String type, String key, BigDecimal amount, JSONObject source)
    {
        JSONObject row = new JSONObject();
        row.put("eventId", key);
        row.put("amountSUM", money(amount == null ? BigDecimal.ZERO : amount));
        if (source == null)
        {
            row.put("eventTime", null);
            row.put("description", "已创建为客户");
            row.put("eventType", type);
            return row;
        }
        row.put("eventTime", formatDateTime(source.getLong("created_at")));
        row.put("eventType", type);
        if ("transaction_created".equals(type))
        {
            row.put("description", "已购买 $" + money(amount) + " USD");
            return row;
        }
        String actionType = source.getString("type");
        if ("sign_up".equals(actionType))
        {
            row.put("description", "执行了操作“sign_up”");
        }
        else
        {
            row.put("description", "执行了操作“" + (StringUtils.hasText(actionType) ? actionType : "-") + "”");
        }
        return row;
    }

    private JSONObject buildDashboard(String partnerStackKey, String fallbackSubId, JSONArray customers,
            JSONArray actions, JSONArray transactions, JSONArray rewards)
    {
        Map<String, JSONObject> rows = new LinkedHashMap<>();
        long paidCustomers = 0;
        for (Object item : customers)
        {
            JSONObject customer = (JSONObject) item;
            JSONObject row = dashboardRow(rows, customer, fallbackSubId);
            row.put("customerCreated", true);
            if (customer.getBooleanValue("has_paid"))
            {
                paidCustomers++;
                row.put("hasPaid", true);
            }
        }

        long actionCount = 0;
        long validActionCount = 0;
        long signupCount = 0;
        long paidSignupCount = 0;
        for (Object item : actions)
        {
            JSONObject action = (JSONObject) item;
            JSONObject row = dashboardRow(rows, action, fallbackSubId);
            long value = action.getLongValue("value", 1L);
            if (value < 1)
            {
                value = 1;
            }
            actionCount += value;
            row.put("actions", row.getLongValue("actions") + value);
            if (!action.getBooleanValue("archived"))
            {
                validActionCount += value;
                row.put("validActions", row.getLongValue("validActions") + value);
            }
            String type = action.getString("type");
            if ("sign_up".equalsIgnoreCase(type))
            {
                signupCount += value;
                row.put("signups", row.getLongValue("signups") + value);
            }
            if ("cash_active".equalsIgnoreCase(type) || "new_high_value_advertiser".equalsIgnoreCase(type))
            {
                paidSignupCount += value;
                row.put("paidSignups", row.getLongValue("paidSignups") + value);
            }
        }

        BigDecimal transactionAmount = BigDecimal.ZERO;
        for (Object item : transactions)
        {
            JSONObject transaction = (JSONObject) item;
            JSONObject row = dashboardRow(rows, transaction, fallbackSubId);
            BigDecimal amount = cents(transaction.containsKey("amount_usd")
                    ? transaction.get("amount_usd") : transaction.get("amount"));
            transactionAmount = transactionAmount.add(amount);
            row.put("transactions", row.getLongValue("transactions") + 1);
            row.put("transactionAmount", money(row.getBigDecimal("transactionAmount").add(amount)));
        }

        BigDecimal rewardAmount = BigDecimal.ZERO;
        for (Object item : rewards)
        {
            JSONObject reward = (JSONObject) item;
            JSONObject row = dashboardRow(rows, reward, fallbackSubId);
            BigDecimal amount = cents(reward.get("amount"));
            rewardAmount = rewardAmount.add(amount);
            row.put("rewards", row.getLongValue("rewards") + 1);
            row.put("rewardAmount", money(row.getBigDecimal("rewardAmount").add(amount)));
        }

        List<JSONObject> sortedRows = aggregateBySubId(rows.values(), fallbackSubId);

        JSONObject summary = new JSONObject();
        summary.put("customers", customers.size());
        summary.put("paidCustomers", paidCustomers);
        summary.put("signups", signupCount);
        summary.put("paidSignups", paidSignupCount);
        summary.put("actions", actionCount);
        summary.put("validActions", validActionCount);
        summary.put("transactions", transactions.size());
        summary.put("transactionAmount", money(transactionAmount));
        summary.put("rewards", rewards.size());
        summary.put("rewardAmount", money(rewardAmount));
        // PartnerStack 的这四个接口没有返回点击字段，保留为 0，避免把动作数伪装成点击数。
        summary.put("rawClicks", 0L);
        summary.put("uniqueClicks", 0L);

        JSONObject result = new JSONObject();
        result.put("partnerStackKey", partnerStackKey);
        result.put("summary", summary);
        result.put("rows", sortedRows);
        result.put("subIds", collectSubIds(fallbackSubId, customers, actions, transactions, rewards));
        return result;
    }

    private JSONObject dashboardRow(Map<String, JSONObject> rows, JSONObject item, String fallbackSubId)
    {
        String customerKey = extractCustomerKey(item);
        if (!StringUtils.hasText(customerKey))
        {
            customerKey = "unknown";
        }
        final String rowKey = customerKey;
        JSONObject row = rows.computeIfAbsent(rowKey, ignored -> {
            JSONObject value = new JSONObject();
            value.put("customerKey", rowKey);
            value.put("subId", fallbackSubId);
            value.put("customerCreated", false);
            value.put("hasPaid", false);
            value.put("signups", 0L);
            value.put("paidSignups", 0L);
            value.put("actions", 0L);
            value.put("validActions", 0L);
            value.put("transactions", 0L);
            value.put("transactionAmount", money(BigDecimal.ZERO));
            value.put("rewards", 0L);
            value.put("rewardAmount", money(BigDecimal.ZERO));
            return value;
        });
        String subId = firstSubId(item);
        if (StringUtils.hasText(subId))
        {
            row.put("subId", subId);
        }
        return row;
    }

    private List<JSONObject> aggregateBySubId(Collection<JSONObject> customerRows, String fallbackSubId)
    {
        Map<String, JSONObject> rows = new LinkedHashMap<>();
        for (JSONObject source : customerRows)
        {
            String subId = StringUtils.hasText(source.getString("subId"))
                    ? source.getString("subId") : fallbackSubId;
            JSONObject row = rows.computeIfAbsent(subId, ignored -> {
                JSONObject value = new JSONObject();
                value.put("subId", subId);
                value.put("rawClicks", 0L);
                value.put("uniqueClicks", 0L);
                value.put("signups", 0L);
                value.put("paidSignups", 0L);
                value.put("actions", 0L);
                value.put("validActions", 0L);
                value.put("transactions", 0L);
                value.put("transactionAmount", money(BigDecimal.ZERO));
                value.put("rewards", 0L);
                value.put("rewardAmount", money(BigDecimal.ZERO));
                return value;
            });
            row.put("signups", row.getLongValue("signups") + source.getLongValue("signups"));
            row.put("paidSignups", row.getLongValue("paidSignups") + source.getLongValue("paidSignups"));
            row.put("actions", row.getLongValue("actions") + source.getLongValue("actions"));
            row.put("validActions", row.getLongValue("validActions") + source.getLongValue("validActions"));
            row.put("transactions", row.getLongValue("transactions") + source.getLongValue("transactions"));
            row.put("rewards", row.getLongValue("rewards") + source.getLongValue("rewards"));
            row.put("transactionAmount", money(row.getBigDecimal("transactionAmount")
                    .add(source.getBigDecimal("transactionAmount"))));
            row.put("rewardAmount", money(row.getBigDecimal("rewardAmount")
                    .add(source.getBigDecimal("rewardAmount"))));
        }
        List<JSONObject> result = new ArrayList<>(rows.values());
        result.sort(Comparator.comparing((JSONObject row) -> row.getBigDecimal("transactionAmount")).reversed());
        return result;
    }

    private JSONArray collectSubIds(String fallbackSubId, JSONArray... sources)
    {
        Set<String> values = new HashSet<>();
        for (JSONArray source : sources)
        {
            for (Object item : source)
            {
                JSONObject object = (JSONObject) item;
                JSONObject customer = object.getJSONObject("customer");
                JSONArray subIds = customer == null ? object.getJSONArray("sub_ids") : customer.getJSONArray("sub_ids");
                if (subIds != null)
                {
                    subIds.forEach(value -> values.add(String.valueOf(value)));
                }
            }
        }
        if (values.isEmpty() && StringUtils.hasText(fallbackSubId))
        {
            values.add(fallbackSubId);
        }
        return new JSONArray(new ArrayList<>(values));
    }

    private String firstSubId(JSONObject item)
    {
        JSONObject customer = item.getJSONObject("customer");
        JSONArray subIds = customer == null ? item.getJSONArray("sub_ids") : customer.getJSONArray("sub_ids");
        return subIds == null || subIds.isEmpty() ? null : subIds.getString(0);
    }

    private BigDecimal cents(Object value)
    {
        if (value == null)
        {
            return BigDecimal.ZERO;
        }
        try
        {
            return new BigDecimal(String.valueOf(value)).movePointLeft(2);
        }
        catch (NumberFormatException e)
        {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal money(BigDecimal value)
    {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private record Scope(String rawKey, String partnershipKey, Set<String> customerKeys,
            boolean requiresCustomerResolution, boolean matchesAll)
    {
        private static Scope all()
        {
            return new Scope("", null, Set.of(), false, true);
        }

        private static Scope from(String rawKey)
        {
            if (rawKey.startsWith("part_"))
            {
                return new Scope(rawKey, rawKey, Set.of(), false, false);
            }
            if (rawKey.startsWith("cus_"))
            {
                return new Scope(rawKey, null, Set.of(rawKey), false, false);
            }
            return new Scope(rawKey, null, Set.of(), true, false);
        }

        private boolean isPartnership()
        {
            return StringUtils.hasText(partnershipKey);
        }

        private String queryCustomerKey()
        {
            return customerKeys.size() == 1 ? customerKeys.iterator().next() : null;
        }

        private Scope withCustomerKeys(Set<String> resolvedCustomerKeys)
        {
            return new Scope(rawKey, null, Set.copyOf(resolvedCustomerKeys), true, false);
        }
    }

    private record PartnerAccess(String token, Scope scope, String displayKey, String fallbackSubId)
    {
    }

    private static class PartnerStackApiException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;
        private final int status;

        PartnerStackApiException(int status, String message)
        {
            super(message);
            this.status = status >= 400 && status <= 599 ? status : 502;
        }

        int getStatus()
        {
            return status;
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
