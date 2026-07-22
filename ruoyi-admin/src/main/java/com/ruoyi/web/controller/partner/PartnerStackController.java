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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
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
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.service.AgentDataScopeService;
import com.ruoyi.system.service.ISysUserService;

/**
 * PartnerStack 数据代理
 */
@RestController
@RequestMapping("/partnerstack")
public class PartnerStackController extends BaseController
{
    private static final int PARTNERSTACK_PAGE_SIZE = 250;
    private static final int MAX_PAGES = 100;
    private static final int REQUEST_TIMEOUT_SECONDS = 12;
    private static final int MAX_REQUEST_ATTEMPTS = 2;
    private static final long RESPONSE_CACHE_TTL_MILLIS = 30_000L;
    private static final List<String> DASHBOARD_SOURCES = List.of("customers", "actions", "transactions", "rewards");
    private static final Map<RequestCacheKey, CachedPartnerResponse> RESPONSE_CACHE = new ConcurrentHashMap<>();
    private static final Map<RequestCacheKey, CompletableFuture<String>> IN_FLIGHT_REQUESTS = new ConcurrentHashMap<>();

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ExecutorService DASHBOARD_EXECUTOR = Executors.newFixedThreadPool(3, runnable ->
    {
        Thread thread = new Thread(runnable, "partnerstack-dashboard");
        thread.setDaemon(true);
        return thread;
    });

    @Value("${partnerstack.base-url:https://api.partnerstack.com/api/v2}")
    private String baseUrl;

    @Value("${partnerstack.token:}")
    private String platformToken;

    private final ISysUserService userService;
    private final AgentDataScopeService agentDataScopeService;

    public PartnerStackController(ISysUserService userService, AgentDataScopeService agentDataScopeService)
    {
        this.userService = userService;
        this.agentDataScopeService = agentDataScopeService;
    }

    /**
     * 首页汇总。PartnerStack 金额字段单位为美分，此接口统一转换为美元。
     */
    @GetMapping("/dashboard")
    public AjaxResult dashboard(@RequestParam(required = false) Long minCreated,
            @RequestParam(required = false) Long maxCreated,
            @RequestParam(required = false) String subId,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String source)
    {
        if (minCreated != null && maxCreated != null && minCreated > maxCreated)
        {
            return error("开始时间不能晚于结束时间");
        }

        List<String> requestedSources;
        try
        {
            requestedSources = dashboardSources(source);
        }
        catch (IllegalArgumentException e)
        {
            return error(e.getMessage());
        }

        PartnerAccess access = scopedPartnerAccess();
        try
        {
            Scope scope = access.scope();
            Map<String, Object> customerParams = new LinkedHashMap<>();
            JSONArray customerSource = new JSONArray();
            boolean loadCustomers = requestedSources.contains("customers");
            if (loadCustomers || scope.requiresCustomerResolution() || StringUtils.hasText(subId))
            {
                if (!scope.requiresCustomerResolution() && !StringUtils.hasText(subId))
                {
                    customerParams = buildCommonParams(minCreated, maxCreated, PARTNERSTACK_PAGE_SIZE, null, null);
                }
                customerSource = fetchAllItems("/customers", customerParams, access.token());
                if (scope.requiresCustomerResolution())
                {
                    scope = resolveScope(scope, customerSource);
                }
            }

            JSONArray customers = loadCustomers
                    ? filterItems(customerSource, scope, subId, minCreated, maxCreated, access.fallbackSubId())
                    : new JSONArray();
            Map<String, Object> eventParams = buildCommonParams(minCreated, maxCreated, PARTNERSTACK_PAGE_SIZE, null, null);
            Set<String> selectedCustomerKeys = StringUtils.hasText(subId)
                    ? matchingCustomerKeys(customerSource, scope, subId, access.fallbackSubId())
                    : Set.of();
            putIfPresent(eventParams, "customer_key", scope.queryCustomerKey());
            JSONArray actionSource = new JSONArray();
            JSONArray transactionSource = new JSONArray();
            JSONArray rewardSource = new JSONArray();
            for (String requestedSource : requestedSources)
            {
                if ("customers".equals(requestedSource))
                {
                    continue;
                }
                JSONArray items = StringUtils.hasText(subId)
                        ? fetchAllItemsForCustomers("/" + requestedSource, eventParams, access.token(), selectedCustomerKeys)
                        : fetchAllItems("/" + requestedSource, eventParams, access.token());
                switch (requestedSource)
                {
                    case "actions" -> actionSource = items;
                    case "transactions" -> transactionSource = items;
                    case "rewards" -> rewardSource = items;
                    default -> throw new IllegalArgumentException("不支持的首页数据源");
                }
            }
            JSONArray actions = filterItems(actionSource, scope, subId, minCreated, maxCreated,
                    access.fallbackSubId());
            JSONArray transactions = filterItems(transactionSource, scope, subId, minCreated, maxCreated,
                    access.fallbackSubId());
            JSONArray rewards = filterItems(rewardSource, scope, subId, minCreated, maxCreated,
                    access.fallbackSubId());

            if (StringUtils.hasText(transactionId))
            {
                customers.clear();
                actions.clear();
                transactions.removeIf(item -> !containsIgnoreCase(((JSONObject) item).getString("key"), transactionId));
                rewards.removeIf(item -> !containsIgnoreCase(((JSONObject) item).getJSONObject("source") == null
                        ? null : ((JSONObject) item).getJSONObject("source").getString("key"), transactionId));
            }

            JSONObject result = buildDashboard(access.displayKey(), access.fallbackSubId(), access.visibleSubIds(), subId,
                    customers, actions, transactions, rewards);
            result.put("loadedSources", requestedSources);
            return success(result);
        }
        catch (PartnerStackApiException e)
        {
            logger.error("PartnerStack dashboard request failed: {}", e.getMessage());
            return AjaxResult.error(e.getStatus(), e.getMessage());
        }
    }

    static List<String> dashboardSources(String source)
    {
        if (!StringUtils.hasText(source))
        {
            return DASHBOARD_SOURCES;
        }
        String normalized = source.trim().toLowerCase();
        if (!DASHBOARD_SOURCES.contains(normalized))
        {
            throw new IllegalArgumentException("不支持的首页数据源: " + source);
        }
        return List.of(normalized);
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
            if (!scope.matchesAll())
            {
                JSONArray customerSource = fetchAllItems("/customers", new LinkedHashMap<>(), access.token());
                if (scope.requiresCustomerResolution())
                {
                    scope = resolveScope(scope, customerSource);
                }
                boolean allowedCustomer = false;
                for (Object item : customerSource)
                {
                    if (item instanceof JSONObject customer
                            && customerKey.equals(customer.getString("key"))
                            && matchesScope(customer, scope))
                    {
                        allowedCustomer = true;
                        break;
                    }
                }
                if (!allowedCustomer)
                {
                    return AjaxResult.error(403, "无权查看该广告户数据");
                }
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
            JSONArray transactions = filterItems(fetchAllItems("/transactions",
                    buildCommonParams(minCreated, maxCreated, PARTNERSTACK_PAGE_SIZE, null, null), access.token()),
                    scope, subId, minCreated, maxCreated, access.fallbackSubId());
            Map<String, BigDecimal> transactionAmounts = sumTransactionAmountsByCustomer(transactions);
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
                BigDecimal amountSum = transactionAmounts.getOrDefault(currentCustomerKey, BigDecimal.ZERO);
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
                row.put("totalRevenue", money(amountSum));
                row.put("createdDate", formatDateOnly(customer.getLong("created_at")));
                row.put("updatedAt", formatIsoDateTime(customer.getLong("updated_at")));
                row.put("createdAt", formatIsoDateTime(customer.getLong("created_at")));
                row.put("amountSUM", money(amountSum));
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
        try
        {
            JSONObject body = executePartnerStackRequest(url, access.token());
            filterByScope(body, access.scope());
            return success(body);
        }
        catch (PartnerStackApiException e)
        {
            logger.error("PartnerStack request failed: {}", e.getMessage());
            return AjaxResult.error(e.getStatus(), e.getMessage());
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
        if (user == null || !StringUtils.hasText(user.getPartnerStackKey()))
        {
            throw new ServiceException("当前用户未绑定 PartnerStack Key，请先在个人中心完成绑定");
        }
        String userKey = user.getPartnerStackKey().trim();
        if (looksLikeAccessToken(userKey))
        {
            return new PartnerAccess(userKey, Scope.all(), maskSecret(userKey), user.getUserName(),
                    agentDataScopeService.selectSubIdsByUserIds(
                            agentDataScopeService.selectSelfAndDescendantUserIds(user.getUserId())));
        }
        if (!StringUtils.hasText(platformToken))
        {
            throw new ServiceException("当前账号绑定的是 PartnerStack 数据Key，但系统未配置 PARTNERSTACK_TOKEN");
        }

        if (SecurityUtils.isAdmin())
        {
            Set<Long> visibleUserIds = new HashSet<>(agentDataScopeService.selectAllAgentUserIds());
            visibleUserIds.add(user.getUserId());
            return new PartnerAccess(platformToken.trim(), Scope.all(), userKey, user.getUserName(),
                    agentDataScopeService.selectSubIdsByUserIds(visibleUserIds));
        }
        Set<Long> visibleUserIds = agentDataScopeService.selectSelfAndDescendantUserIds(user.getUserId());
        Set<String> attributionKeys = new HashSet<>(agentDataScopeService.selectPartnerAttributionKeys(visibleUserIds));
        attributionKeys.add(userKey);
        return new PartnerAccess(platformToken.trim(), Scope.from(attributionKeys),
                String.join(",", attributionKeys), user.getUserName(),
                agentDataScopeService.selectSubIdsByUserIds(visibleUserIds));
    }

    static boolean looksLikeAccessToken(String value)
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

    private static String extractCustomerKey(Object item)
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

    private JSONArray fetchAllItemsForCustomers(String path, Map<String, Object> sourceParams, String accessToken,
            Set<String> customerKeys)
    {
        JSONArray result = new JSONArray();
        for (String customerKey : customerKeys)
        {
            Map<String, Object> params = new LinkedHashMap<>(sourceParams);
            params.put("customer_key", customerKey);
            result.addAll(fetchAllItems(path, params, accessToken));
        }
        return result;
    }

    static <T> List<T> runInParallel(List<Supplier<T>> suppliers)
    {
        List<CompletableFuture<T>> futures = suppliers.stream()
                .map(supplier -> CompletableFuture.supplyAsync(supplier, DASHBOARD_EXECUTOR))
                .toList();
        try
        {
            return futures.stream().map(CompletableFuture::join).toList();
        }
        catch (CompletionException e)
        {
            futures.forEach(future -> future.cancel(true));
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException)
            {
                throw runtimeException;
            }
            throw new IllegalStateException(cause);
        }
    }

    private JSONObject requestPartnerStack(String path, Map<String, Object> params, String accessToken)
    {
        String query = buildQuery(params);
        String url = baseUrl + path + (query.isEmpty() ? "" : "?" + query);
        return executePartnerStackRequest(url, accessToken);
    }

    JSONObject executePartnerStackRequest(String url, String accessToken)
    {
        RequestCacheKey cacheKey = new RequestCacheKey(url, accessToken);
        long now = System.currentTimeMillis();
        CachedPartnerResponse cached = RESPONSE_CACHE.get(cacheKey);
        if (cached != null && cached.expiresAt() > now)
        {
            return JSON.parseObject(cached.body());
        }
        if (cached != null)
        {
            RESPONSE_CACHE.remove(cacheKey, cached);
        }

        CompletableFuture<String> newRequest = new CompletableFuture<>();
        CompletableFuture<String> request = IN_FLIGHT_REQUESTS.putIfAbsent(cacheKey, newRequest);
        if (request == null)
        {
            request = newRequest;
            try
            {
                String body = sendPartnerStackRequest(url, accessToken);
                RESPONSE_CACHE.put(cacheKey,
                        new CachedPartnerResponse(body, System.currentTimeMillis() + RESPONSE_CACHE_TTL_MILLIS));
                newRequest.complete(body);
                removeExpiredCacheEntries();
            }
            catch (RuntimeException e)
            {
                newRequest.completeExceptionally(e);
            }
            finally
            {
                IN_FLIGHT_REQUESTS.remove(cacheKey, newRequest);
            }
        }

        try
        {
            return JSON.parseObject(request.join());
        }
        catch (CompletionException e)
        {
            Throwable cause = e.getCause();
            if (cause instanceof PartnerStackApiException apiException)
            {
                throw apiException;
            }
            throw new PartnerStackApiException(502, "PartnerStack 接口调用失败");
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

    private String sendPartnerStackRequest(String url, String accessToken)
    {
        PartnerStackApiException lastError = null;
        for (int attempt = 1; attempt <= MAX_REQUEST_ATTEMPTS; attempt++)
        {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            long startedAt = System.nanoTime();
            try
            {
                HttpResponse<String> response = HTTP_CLIENT.send(request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                long elapsedMillis = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
                if (elapsedMillis >= 3_000)
                {
                    logger.warn("Slow PartnerStack response: path={}, status={}, elapsedMs={}, attempt={}",
                            URI.create(url).getPath(), response.statusCode(), elapsedMillis, attempt);
                }
                if (response.statusCode() >= 200 && response.statusCode() < 300)
                {
                    JSONObject body = JSON.parseObject(response.body());
                    if (body == null)
                    {
                        throw new PartnerStackApiException(502, "PartnerStack 响应格式错误");
                    }
                    return response.body();
                }
                JSONObject body = JSON.parseObject(response.body());
                String message = body != null && StringUtils.hasText(body.getString("message"))
                        ? body.getString("message") : "PartnerStack 请求失败";
                lastError = new PartnerStackApiException(502,
                        "PartnerStack 请求失败（HTTP " + response.statusCode() + "）: " + message);
                if (!isTransientStatus(response.statusCode()) || attempt == MAX_REQUEST_ATTEMPTS)
                {
                    throw lastError;
                }
            }
            catch (IOException e)
            {
                lastError = new PartnerStackApiException(502, "PartnerStack 接口调用失败: " + e.getMessage());
                if (attempt == MAX_REQUEST_ATTEMPTS)
                {
                    throw lastError;
                }
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new PartnerStackApiException(502, "PartnerStack 接口调用被中断");
            }
            if (!sleepBeforeRetry(attempt))
            {
                throw new PartnerStackApiException(502, "PartnerStack 接口调用被中断");
            }
        }
        throw lastError == null ? new PartnerStackApiException(502, "PartnerStack 接口调用失败") : lastError;
    }

    private boolean isTransientStatus(int status)
    {
        return status == 408 || status == 429 || status >= 500;
    }

    private boolean sleepBeforeRetry(int attempt)
    {
        try
        {
            Thread.sleep(250L * attempt);
            return true;
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void removeExpiredCacheEntries()
    {
        if (RESPONSE_CACHE.size() <= 500)
        {
            return;
        }
        long now = System.currentTimeMillis();
        RESPONSE_CACHE.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
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
            if (matchesAttribution(customer, unresolved.attributionKeys()))
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

    static Set<String> matchingCustomerKeys(JSONArray customers, Scope scope, String subId, String fallbackSubId)
    {
        Set<String> customerKeys = new HashSet<>();
        for (Object item : customers)
        {
            if (!(item instanceof JSONObject customer)
                    || !matchesScope(customer, scope)
                    || !matchesSubIdValue(customer, subId, fallbackSubId))
            {
                continue;
            }
            String customerKey = customer.getString("key");
            if (StringUtils.hasText(customerKey))
            {
                customerKeys.add(customerKey);
            }
        }
        return customerKeys;
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

    private static boolean matchesScope(JSONObject item, Scope scope)
    {
        if (scope.matchesAll())
        {
            return true;
        }
        if (containsKey(scope.partnershipKeys(), item.getString("partnership_key")))
        {
            return true;
        }
        String customerKey = extractCustomerKey(item);
        if (containsKey(scope.customerKeys(), customerKey))
        {
            return true;
        }
        return matchesAttribution(item, scope.attributionKeys());
    }

    private static boolean matchesAttribution(JSONObject item, Set<String> attributionKeys)
    {
        if (item == null || attributionKeys == null || attributionKeys.isEmpty())
        {
            return false;
        }
        if (containsKey(attributionKeys, item.getString("customer_key"))
                || containsKey(attributionKeys, item.getString("shared_id")))
        {
            return true;
        }
        JSONObject customer = item.getJSONObject("customer");
        if (customer != null && matchesAttribution(customer, attributionKeys))
        {
            return true;
        }
        JSONArray subIds = item.getJSONArray("sub_ids");
        return subIds != null && subIds.stream().anyMatch(value -> attributionKeys.contains(String.valueOf(value)));
    }

    private static boolean containsKey(Set<String> keys, String value)
    {
        return StringUtils.hasText(value) && keys.contains(value);
    }

    private boolean matchesSubId(JSONObject item, String subId, String fallbackSubId)
    {
        return matchesSubIdValue(item, subId, fallbackSubId);
    }

    private static boolean matchesSubIdValue(JSONObject item, String subId, String fallbackSubId)
    {
        if (!StringUtils.hasText(subId))
        {
            return true;
        }
        JSONObject customer = item.getJSONObject("customer");
        JSONArray subIds = customer == null ? item.getJSONArray("sub_ids") : customer.getJSONArray("sub_ids");
        return containsValue(subIds, subId)
                || ((subIds == null || subIds.isEmpty()) && subId.equals(fallbackSubId));
    }

    private boolean contains(JSONArray values, String expected)
    {
        return containsValue(values, expected);
    }

    private static boolean containsValue(JSONArray values, String expected)
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

    static Map<String, BigDecimal> sumTransactionAmountsByCustomer(JSONArray transactions)
    {
        Map<String, BigDecimal> amounts = new LinkedHashMap<>();
        for (Object item : transactions)
        {
            if (!(item instanceof JSONObject transaction))
            {
                continue;
            }
            String customerKey = extractCustomerKey(transaction);
            if (!StringUtils.hasText(customerKey))
            {
                continue;
            }
            BigDecimal amount = cents(transaction.containsKey("amount_usd")
                    ? transaction.get("amount_usd") : transaction.get("amount"));
            amounts.merge(customerKey, amount, BigDecimal::add);
        }
        amounts.replaceAll((key, value) -> money(value));
        return amounts;
    }

    static <T> List<T> pageRows(List<T> rows, int pageNum, int pageSize)
    {
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.max(pageSize, 1);
        int fromIndex = Math.min((safePageNum - 1) * safePageSize, rows.size());
        int toIndex = Math.min(fromIndex + safePageSize, rows.size());
        return rows.subList(fromIndex, toIndex);
    }

    static boolean matchesPartnerAttribution(JSONObject item, Collection<String> attributionKeys)
    {
        return matchesScope(item, Scope.from(attributionKeys));
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

    private JSONObject buildDashboard(String partnerStackKey, String fallbackSubId, Set<String> visibleSubIds,
            String selectedSubId, JSONArray customers, JSONArray actions, JSONArray transactions, JSONArray rewards)
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
        addMissingSubIdRows(sortedRows, visibleSubIds, selectedSubId);

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
        JSONArray subIds = collectSubIds(fallbackSubId, customers, actions, transactions, rewards);
        visibleSubIds.forEach(value -> {
            if (!subIds.contains(value))
            {
                subIds.add(value);
            }
        });
        result.put("subIds", subIds);
        return result;
    }

    static void addMissingSubIdRows(List<JSONObject> rows, Collection<String> visibleSubIds, String selectedSubId)
    {
        Set<String> existingSubIds = new HashSet<>();
        rows.forEach(row -> existingSubIds.add(row.getString("subId")));
        for (String subId : visibleSubIds)
        {
            if (!StringUtils.hasText(subId)
                    || (StringUtils.hasText(selectedSubId) && !selectedSubId.equals(subId))
                    || !existingSubIds.add(subId))
            {
                continue;
            }
            JSONObject row = new JSONObject();
            row.put("subId", subId);
            row.put("rawClicks", 0L);
            row.put("uniqueClicks", 0L);
            row.put("signups", 0L);
            row.put("paidSignups", 0L);
            row.put("actions", 0L);
            row.put("validActions", 0L);
            row.put("transactions", 0L);
            row.put("transactionAmount", BigDecimal.ZERO.setScale(2));
            row.put("rewards", 0L);
            row.put("rewardAmount", BigDecimal.ZERO.setScale(2));
            rows.add(row);
        }
        rows.sort(Comparator.comparing((JSONObject row) -> row.getBigDecimal("transactionAmount")).reversed()
                .thenComparing(row -> row.getString("subId"), Comparator.nullsLast(String::compareTo)));
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

    private static BigDecimal cents(Object value)
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

    private static BigDecimal money(BigDecimal value)
    {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    record Scope(Set<String> attributionKeys, Set<String> partnershipKeys, Set<String> customerKeys,
            boolean requiresCustomerResolution, boolean matchesAll)
    {
        static Scope all()
        {
            return new Scope(Set.of(), Set.of(), Set.of(), false, true);
        }

        private static Scope from(Collection<String> rawKeys)
        {
            Set<String> attributionKeys = new HashSet<>();
            Set<String> partnershipKeys = new HashSet<>();
            Set<String> customerKeys = new HashSet<>();
            if (rawKeys != null)
            {
                for (String rawKey : rawKeys)
                {
                    if (!StringUtils.hasText(rawKey))
                    {
                        continue;
                    }
                    String key = rawKey.trim();
                    attributionKeys.add(key);
                    if (key.startsWith("part_"))
                    {
                        partnershipKeys.add(key);
                    }
                    else if (key.startsWith("cus_"))
                    {
                        customerKeys.add(key);
                    }
                }
            }
            return new Scope(Set.copyOf(attributionKeys), Set.copyOf(partnershipKeys), Set.copyOf(customerKeys),
                    !attributionKeys.isEmpty(), false);
        }

        private String queryCustomerKey()
        {
            return customerKeys.size() == 1 ? customerKeys.iterator().next() : null;
        }

        private Scope withCustomerKeys(Set<String> resolvedCustomerKeys)
        {
            Set<String> combinedCustomerKeys = new HashSet<>(customerKeys);
            combinedCustomerKeys.addAll(resolvedCustomerKeys);
            return new Scope(attributionKeys, partnershipKeys, Set.copyOf(combinedCustomerKeys), false, false);
        }
    }

    private record PartnerAccess(String token, Scope scope, String displayKey, String fallbackSubId,
            Set<String> visibleSubIds)
    {
    }

    private record RequestCacheKey(String url, String accessToken)
    {
    }

    private record CachedPartnerResponse(String body, long expiresAt)
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
