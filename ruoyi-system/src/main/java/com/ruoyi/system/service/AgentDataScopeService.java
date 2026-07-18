package com.ruoyi.system.service;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.AgentClient;
import com.ruoyi.system.mapper.AgentClientMapper;
import com.ruoyi.system.mapper.AgentSubIdMapper;

/**
 * Resolves the application-level agent tree. PartnerStack only authenticates the
 * platform account; it does not enforce this hierarchy for us.
 */
@Service
public class AgentDataScopeService
{
    private final AgentClientMapper agentClientMapper;
    private final AgentSubIdMapper agentSubIdMapper;

    public AgentDataScopeService(AgentClientMapper agentClientMapper, AgentSubIdMapper agentSubIdMapper)
    {
        this.agentClientMapper = agentClientMapper;
        this.agentSubIdMapper = agentSubIdMapper;
    }

    /** Current user plus every descendant, regardless of nesting depth. */
    public Set<Long> selectSelfAndDescendantUserIds(Long userId)
    {
        if (userId == null)
        {
            return Collections.emptySet();
        }
        return collectSelfAndDescendants(userId, agentClientMapper.selectAllAgentRelations());
    }

    /** Every descendant, excluding the current user. */
    public Set<Long> selectDescendantUserIds(Long userId)
    {
        Set<Long> userIds = new LinkedHashSet<>(selectSelfAndDescendantUserIds(userId));
        userIds.remove(userId);
        return userIds;
    }

    public Set<Long> selectAllAgentUserIds()
    {
        Set<Long> userIds = new LinkedHashSet<>();
        for (AgentClient agent : agentClientMapper.selectAllAgentRelations())
        {
            if (agent != null && agent.getSysUserId() != null)
            {
                userIds.add(agent.getSysUserId());
            }
        }
        return userIds;
    }

    /**
     * All PartnerStack attribution values owned by users in this scope. Values
     * can be partnership keys, customer keys, external customer keys, or SubIds.
     */
    public Set<String> selectPartnerAttributionKeys(Collection<Long> userIds)
    {
        if (userIds == null || userIds.isEmpty())
        {
            return Collections.emptySet();
        }
        Set<String> keys = new LinkedHashSet<>();
        addNonBlank(keys, agentClientMapper.selectPartnerKeysByUserIds(userIds));
        addNonBlank(keys, agentSubIdMapper.selectSubIdsByUserIds(userIds));
        return keys;
    }

    /** Active SubIds owned by users in this scope, including SubIds with no PartnerStack activity yet. */
    public Set<String> selectSubIdsByUserIds(Collection<Long> userIds)
    {
        if (userIds == null || userIds.isEmpty())
        {
            return Collections.emptySet();
        }
        Set<String> subIds = new LinkedHashSet<>();
        addNonBlank(subIds, agentSubIdMapper.selectSubIdsByUserIds(userIds));
        return subIds;
    }

    static Set<Long> collectSelfAndDescendants(Long userId, List<AgentClient> agents)
    {
        Map<Long, Set<Long>> childrenByOwner = new HashMap<>();
        if (agents != null)
        {
            for (AgentClient agent : agents)
            {
                if (agent == null || agent.getOwnerUserId() == null || agent.getSysUserId() == null)
                {
                    continue;
                }
                childrenByOwner.computeIfAbsent(agent.getOwnerUserId(), ignored -> new LinkedHashSet<>())
                        .add(agent.getSysUserId());
            }
        }

        Set<Long> result = new LinkedHashSet<>();
        ArrayDeque<Long> pending = new ArrayDeque<>();
        pending.add(userId);
        while (!pending.isEmpty())
        {
            Long current = pending.removeFirst();
            if (!result.add(current))
            {
                continue;
            }
            for (Long child : childrenByOwner.getOrDefault(current, Collections.emptySet()))
            {
                if (!result.contains(child))
                {
                    pending.addLast(child);
                }
            }
        }
        return result;
    }

    private void addNonBlank(Set<String> target, List<String> values)
    {
        if (values == null)
        {
            return;
        }
        for (String value : values)
        {
            if (StringUtils.isNotBlank(value))
            {
                target.add(value.trim());
            }
        }
    }
}
