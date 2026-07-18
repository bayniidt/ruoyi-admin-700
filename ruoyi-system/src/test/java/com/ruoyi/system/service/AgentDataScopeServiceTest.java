package com.ruoyi.system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import com.ruoyi.system.domain.AgentClient;

class AgentDataScopeServiceTest
{
    @Test
    void collectsSelfAndEveryDescendantWithoutIncludingOtherBranches()
    {
        List<AgentClient> agents = List.of(
                agent(1L, 2L),
                agent(2L, 3L),
                agent(2L, 4L),
                agent(3L, 5L),
                agent(9L, 10L));

        assertEquals(Set.of(2L, 3L, 4L, 5L),
                AgentDataScopeService.collectSelfAndDescendants(2L, agents));
    }

    @Test
    void terminatesSafelyWhenLegacyDataContainsACycle()
    {
        List<AgentClient> agents = List.of(agent(1L, 2L), agent(2L, 1L));

        assertEquals(Set.of(1L, 2L), AgentDataScopeService.collectSelfAndDescendants(1L, agents));
    }

    private AgentClient agent(Long ownerUserId, Long sysUserId)
    {
        AgentClient agent = new AgentClient();
        agent.setOwnerUserId(ownerUserId);
        agent.setSysUserId(sysUserId);
        return agent;
    }
}
