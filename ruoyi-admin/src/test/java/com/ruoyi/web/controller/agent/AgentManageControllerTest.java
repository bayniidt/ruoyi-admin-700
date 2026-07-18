package com.ruoyi.web.controller.agent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.Test;
import com.ruoyi.system.service.AgentClientService;
import com.ruoyi.system.service.AgentDataScopeService;

class AgentManageControllerTest
{
    @Test
    void managementScopeContainsDescendantsButNotCurrentAgent()
    {
        AgentDataScopeService dataScopeService = mock(AgentDataScopeService.class);
        when(dataScopeService.selectDescendantUserIds(2L)).thenReturn(Set.of(3L, 4L, 5L));
        AgentManageController controller = new AgentManageController(mock(AgentClientService.class), dataScopeService);

        Set<Long> visibleUserIds = controller.resolveManagedAgentUserIds(false, 2L);

        assertFalse(visibleUserIds.contains(2L));
        assertTrue(visibleUserIds.containsAll(Set.of(3L, 4L, 5L)));
    }

    @Test
    void adminManagementScopeRemainsUnrestricted()
    {
        AgentManageController controller = new AgentManageController(
                mock(AgentClientService.class), mock(AgentDataScopeService.class));

        assertNull(controller.resolveManagedAgentUserIds(true, 1L));
    }
}
