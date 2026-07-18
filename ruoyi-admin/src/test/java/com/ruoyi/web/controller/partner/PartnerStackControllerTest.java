package com.ruoyi.web.controller.partner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import com.alibaba.fastjson2.JSONObject;

class PartnerStackControllerTest
{
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
}
