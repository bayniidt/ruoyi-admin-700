package com.ruoyi.web.controller.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.Test;
import com.ruoyi.system.service.AgentClientService;
import com.ruoyi.system.service.AgentDataScopeService;
import com.ruoyi.system.service.AgentSubIdService;
import com.ruoyi.system.service.ISysUserService;

class AgentSubIdControllerTest
{
    @Test
    void ordinaryAgentCanAssignOnlyToDescendantsAndManageOnlyItsSubtree()
    {
        AgentDataScopeService dataScopeService = mock(AgentDataScopeService.class);
        when(dataScopeService.selectDescendantUserIds(2L)).thenReturn(Set.of(3L, 4L));
        when(dataScopeService.selectSelfAndDescendantUserIds(2L)).thenReturn(Set.of(2L, 3L, 4L));
        AgentSubIdController controller = controller(dataScopeService);

        assertEquals(Set.of(3L, 4L), controller.resolveAssignableOwnerUserIds(false, 2L));
        assertEquals(Set.of(2L, 3L, 4L), controller.resolveManageableOwnerUserIds(false, 2L));
    }

    @Test
    void adminCanAssignToEveryAgentAndManageEverySubId()
    {
        AgentDataScopeService dataScopeService = mock(AgentDataScopeService.class);
        when(dataScopeService.selectAllAgentUserIds()).thenReturn(Set.of(2L, 3L, 4L));
        AgentSubIdController controller = controller(dataScopeService);

        assertEquals(Set.of(1L, 2L, 3L, 4L), controller.resolveAssignableOwnerUserIds(true, 1L));
        assertNull(controller.resolveManageableOwnerUserIds(true, 1L));
    }

    private AgentSubIdController controller(AgentDataScopeService dataScopeService)
    {
        return new AgentSubIdController(mock(AgentSubIdService.class), dataScopeService,
                mock(AgentClientService.class), mock(ISysUserService.class));
    }
}
