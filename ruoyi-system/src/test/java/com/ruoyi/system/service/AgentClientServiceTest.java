package com.ruoyi.system.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.AgentClient;
import com.ruoyi.system.mapper.AgentClientMapper;

class AgentClientServiceTest
{
    @Test
    void changingPasswordUpdatesLoginUserAndAgentPasswordHash()
    {
        AgentClientMapper mapper = mock(AgentClientMapper.class);
        ISysUserService userService = mock(ISysUserService.class);
        AgentClientService service = new AgentClientService(mapper, userService);
        AgentClient agent = new AgentClient();
        agent.setAgentId(12L);
        agent.setSysUserId(34L);
        when(mapper.selectAgentById(12L)).thenReturn(agent);
        when(userService.resetPwd(any(SysUser.class))).thenReturn(1);
        when(mapper.updatePasswordHash(eq(12L), anyString())).thenReturn(1);

        service.changePassword(12L, "newPass123");

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).resetPwd(userCaptor.capture());
        assertTrue(SecurityUtils.matchesPassword("newPass123", userCaptor.getValue().getPassword()));
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(mapper).updatePasswordHash(eq(12L), hashCaptor.capture());
        assertTrue(SecurityUtils.matchesPassword("newPass123", hashCaptor.getValue()));
    }
}
