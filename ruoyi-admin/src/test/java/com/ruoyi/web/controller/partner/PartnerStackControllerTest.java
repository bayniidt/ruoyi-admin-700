package com.ruoyi.web.controller.partner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.mock.web.MockHttpServletResponse;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.sun.net.httpserver.HttpServer;

class PartnerStackControllerTest
{
    @Test
    void selectsDashboardSourcesForProgressiveLoading()
    {
        assertEquals(List.of("customers", "actions", "transactions", "rewards"),
                PartnerStackController.dashboardSources(null));
        assertEquals(List.of("actions"), PartnerStackController.dashboardSources(" Actions "));
        assertThrows(IllegalArgumentException.class,
                () -> PartnerStackController.dashboardSources("unknown"));
    }

    @Test
    void cachesSuccessfulPartnerStackResponsesAndReturnsIndependentCopies() throws Exception
    {
        AtomicInteger upstreamRequests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/actions", exchange -> {
            upstreamRequests.incrementAndGet();
            byte[] body = "{\"data\":{\"items\":[{\"key\":\"act_1\"}]}}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try
        {
            PartnerStackController controller = new PartnerStackController(null, null);
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/actions?test=" + System.nanoTime();

            JSONObject first = controller.executePartnerStackRequest(url, "test-token");
            first.getJSONObject("data").getJSONArray("items").clear();
            JSONObject second = controller.executePartnerStackRequest(url, "test-token");

            assertEquals(1, upstreamRequests.get());
            assertEquals(1, second.getJSONObject("data").getJSONArray("items").size());
        }
        finally
        {
            server.stop(0);
        }
    }

    @Test
    void retriesTransientPartnerStackFailures() throws Exception
    {
        AtomicInteger upstreamRequests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/transactions", exchange -> {
            int requestNumber = upstreamRequests.incrementAndGet();
            byte[] body = (requestNumber == 1
                    ? "{\"message\":\"temporary upstream failure\"}"
                    : "{\"data\":{\"items\":[]}}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(requestNumber == 1 ? 503 : 200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try
        {
            PartnerStackController controller = new PartnerStackController(null, null);
            String url = "http://127.0.0.1:" + server.getAddress().getPort()
                    + "/transactions?test=" + System.nanoTime();

            JSONObject response = controller.executePartnerStackRequest(url, "test-token");

            assertEquals(2, upstreamRequests.get());
            assertTrue(response.getJSONObject("data").getJSONArray("items").isEmpty());
        }
        finally
        {
            server.stop(0);
        }
    }

    @Test
    void distinguishesApiTokensFromPartnerStackDataKeys()
    {
        assertTrue(PartnerStackController.looksLikeAccessToken(
                "fay2dGIxZKSls3K5USkVs0eGZ7N10mkuMytLrMbzDObrFglXoZenMfw8TqAGdryt"));
        assertFalse(PartnerStackController.looksLikeAccessToken("part_example"));
        assertFalse(PartnerStackController.looksLikeAccessToken("cus_example"));
        assertFalse(PartnerStackController.looksLikeAccessToken("short-key"));
    }

    @Test
    void matchesAdvertiserIdAcrossPartnerStackCustomerFields()
    {
        JSONObject customer = new JSONObject();
        customer.put("key", "cus_Ak7az1vjG4iNux");
        customer.put("customer_key", "7662651690235002887");
        customer.put("shared_id", "external-advertiser-id");

        assertTrue(PartnerStackController.matchesAdAccountId(customer, "7662651690235002887"));
        assertTrue(PartnerStackController.matchesAdAccountId(customer, "ak7AZ1"));
        assertTrue(PartnerStackController.matchesAdAccountId(customer, "ADVERTISER"));
        assertFalse(PartnerStackController.matchesAdAccountId(customer, "missing"));
    }

    @Test
    void sumsEffectiveSpendFromTransactionsInsteadOfMissingCustomerAmountFields()
    {
        JSONArray transactions = JSONArray.of(
                transaction("cus_RZ3srbPLldtwQT", 279),
                transaction("cus_RZ3srbPLldtwQT", 394),
                transaction("cus_RZ3srbPLldtwQT", 12),
                transaction("cus_other", 36980));

        assertEquals(new BigDecimal("6.85"),
                PartnerStackController.sumTransactionAmountsByCustomer(transactions)
                        .get("cus_RZ3srbPLldtwQT"));
        assertEquals(new BigDecimal("369.80"),
                PartnerStackController.sumTransactionAmountsByCustomer(transactions).get("cus_other"));
    }

    @Test
    void paginatesAllThirtyThreeAdvertisersWithoutDroppingTheLastPage()
    {
        List<Integer> rows = new ArrayList<>(IntStream.rangeClosed(1, 33).boxed().toList());

        assertEquals(IntStream.rangeClosed(1, 10).boxed().toList(),
                PartnerStackController.pageRows(rows, 1, 10));
        assertEquals(IntStream.rangeClosed(11, 20).boxed().toList(),
                PartnerStackController.pageRows(rows, 2, 10));
        assertEquals(List.of(31, 32, 33), PartnerStackController.pageRows(rows, 4, 10));
        assertTrue(PartnerStackController.pageRows(rows, 5, 10).isEmpty());
    }

    @Test
    void matchesOnlyPartnershipCustomerOrSubIdInsideTheAgentScope()
    {
        JSONObject event = new JSONObject();
        event.put("partnership_key", "part_branch_b");
        event.put("customer", JSONObject.of(
                "key", "cus_branch_b",
                "sub_ids", List.of("sub_branch_b")));

        assertTrue(PartnerStackController.matchesPartnerAttribution(event, Set.of("part_branch_b")));
        assertTrue(PartnerStackController.matchesPartnerAttribution(event, Set.of("cus_branch_b")));
        assertTrue(PartnerStackController.matchesPartnerAttribution(event, Set.of("sub_branch_b")));
        assertFalse(PartnerStackController.matchesPartnerAttribution(event, Set.of("sub_other_branch")));
    }

    @Test
    void narrowsDashboardEventsToCustomersMatchingSelectedSubId()
    {
        JSONObject matching = JSONObject.of("key", "cus_test33", "sub_ids", JSONArray.of("test33"));
        JSONObject other = JSONObject.of("key", "cus_other", "sub_ids", JSONArray.of("other"));

        Set<String> customerKeys = PartnerStackController.matchingCustomerKeys(
                JSONArray.of(matching, other), PartnerStackController.Scope.all(), "test33", "fallback");

        assertEquals(Set.of("cus_test33"), customerKeys);
    }

    @Test
    void returnsNoCustomerKeysWhenSelectedSubIdDoesNotExist()
    {
        JSONObject customer = JSONObject.of("key", "cus_other", "sub_ids", JSONArray.of("other"));

        Set<String> customerKeys = PartnerStackController.matchingCustomerKeys(
                JSONArray.of(customer), PartnerStackController.Scope.all(), "missing", "fallback");

        assertTrue(customerKeys.isEmpty());
    }

    @Test
    void addsZeroValueRowsForVisibleSubIdsWithoutPartnerStackActivity()
    {
        List<JSONObject> rows = new ArrayList<>();

        PartnerStackController.addMissingSubIdRows(rows, Set.of("manhuang", "manhuang1"), null);

        assertEquals(2, rows.size());
        assertEquals(Set.of("manhuang", "manhuang1"),
                rows.stream().map(row -> row.getString("subId")).collect(java.util.stream.Collectors.toSet()));
        assertTrue(rows.stream().allMatch(row -> row.getLongValue("actions") == 0));
        assertTrue(rows.stream().allMatch(row -> row.getBigDecimal("rewardAmount").signum() == 0));
    }

    @Test
    void excludesActionRewardsFromDashboardCommission()
    {
        JSONObject transactionReward = JSONObject.of(
                "amount", 17347,
                "source", JSONObject.of("type", "transaction"));
        JSONObject actionReward = JSONObject.of(
                "amount", 44000,
                "source", JSONObject.of("type", "action"));

        JSONArray dashboardRewards = PartnerStackController.dashboardCommissionRewards(
                JSONArray.of(transactionReward, actionReward));

        assertEquals(1, dashboardRewards.size());
        assertEquals(transactionReward, dashboardRewards.getJSONObject(0));
    }

    @Test
    void zeroValueRowsRespectSelectedSubIdFilter()
    {
        List<JSONObject> rows = new ArrayList<>();

        PartnerStackController.addMissingSubIdRows(rows, Set.of("manhuang", "manhuang1"), "manhuang1");

        assertEquals(1, rows.size());
        assertEquals("manhuang1", rows.get(0).getString("subId"));
    }

    @Test
    void loadsIndependentDashboardSourcesInParallel()
    {
        CountDownLatch started = new CountDownLatch(3);
        Supplier<Integer> first = concurrentSource(started, 1);
        Supplier<Integer> second = concurrentSource(started, 2);
        Supplier<Integer> third = concurrentSource(started, 3);

        assertEquals(List.of(1, 2, 3), PartnerStackController.runInParallel(List.of(first, second, third)));
    }

    @Test
    void mapsTransactionKeyAndActualRewardStatusLikeReferenceExport()
    {
        JSONObject transaction = JSONObject.of(
                "key", "7669986365668474896_101_1_20260818",
                "customer", JSONObject.of("key", "cus_partnerstack"));
        JSONObject pending = JSONObject.of(
                "reward_status", "pending",
                "updated_at", 100L,
                "source", JSONObject.of("type", "transaction", "key", transaction.getString("key")));
        JSONObject approved = JSONObject.of(
                "reward_status", "approved",
                "updated_at", 200L,
                "source", JSONObject.of("type", "transaction", "key", transaction.getString("key")));

        assertEquals("7669986365668474896", PartnerStackController.transactionCustomerKey(transaction));
        assertEquals("approved", PartnerStackController.transactionRewardStatuses(
                JSONArray.of(pending, approved)).get(transaction.getString("key")));
        assertEquals("待审核", PartnerStackController.rewardStatusLabel("pending"));
        assertEquals("已通过", PartnerStackController.rewardStatusLabel("approved"));
        assertEquals("待审核", PartnerStackController.rewardStatusLabel("paid"));
        assertEquals("待审核", PartnerStackController.rewardStatusLabel(null));
    }

    @Test
    void exportColumnsMatchReferenceWorkbookOrder()
    {
        List<String> headers = Arrays.stream(PartnerStackController.ActionRecordRow.class.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Excel.class))
                .sorted(Comparator.comparingInt(field -> field.getAnnotation(Excel.class).sort()))
                .map(field -> field.getAnnotation(Excel.class).name())
                .toList();

        assertEquals(List.of("交易 Key", "客户 Key", "SubId", "状态", "有效消耗(美分)", "时间"), headers);
    }

    @Test
    void convertsExportCentsToDialogDollars()
    {
        PartnerStackController.ActionRecordRow row = new PartnerStackController.ActionRecordRow(
                "transaction", "customer", "fuhao", "待审核", new BigDecimal("2505"),
                "2026-08-19 06:00:00");

        assertEquals(new BigDecimal("25.05"), row.getAmountUsd());
    }

    @Test
    void generatedWorkbookMatchesReferenceSheetAndFields() throws Exception
    {
        PartnerStackController.ActionRecordRow row = new PartnerStackController.ActionRecordRow(
                "7669986365668474896_101_1_20260818", "7669986365668474896", "fuhao", "待审核",
                new BigDecimal("2505"), "2026-08-19 06:00:00");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new ExcelUtil<>(PartnerStackController.ActionRecordRow.class)
                .exportExcel(response, List.of(row), "【交易信息】数据");

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray())))
        {
            var sheet = workbook.getSheetAt(0);
            assertEquals("【交易信息】数据", sheet.getSheetName());
            assertEquals(List.of("交易 Key", "客户 Key", "SubId", "状态", "有效消耗(美分)", "时间"),
                    IntStream.range(0, 6).mapToObj(index -> sheet.getRow(0).getCell(index).getStringCellValue())
                            .toList());
            assertEquals("7669986365668474896_101_1_20260818", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("7669986365668474896", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals(2505D, sheet.getRow(1).getCell(4).getNumericCellValue());
            assertEquals("2026-08-19 06:00:00", sheet.getRow(1).getCell(5).getStringCellValue());
        }
    }

    private Supplier<Integer> concurrentSource(CountDownLatch started, int result)
    {
        return () ->
        {
            started.countDown();
            try
            {
                if (!started.await(2, TimeUnit.SECONDS))
                {
                    throw new IllegalStateException("dashboard sources were executed sequentially");
                }
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
            return result;
        };
    }

    private JSONObject transaction(String customerKey, int amountUsd)
    {
        return JSONObject.of(
                "customer", JSONObject.of("key", customerKey),
                "amount_usd", amountUsd);
    }
}
