package com.ruoyi.system.service;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.AgentClient;
import com.ruoyi.system.domain.AgentSubId;
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

    /** Commission rates keyed by the PartnerStack customer key and owned SubId. */
    public Map<String, BigDecimal> selectCommissionRatesByUserIds(Collection<Long> userIds)
    {
        if (userIds == null || userIds.isEmpty())
        {
            return Collections.emptyMap();
        }
        Map<Long, BigDecimal> ratesByUserId = new HashMap<>();
        Map<String, BigDecimal> ratesByAttributionKey = new HashMap<>();
        for (AgentClient agent : agentClientMapper.selectAgentList(userIds, null))
        {
            if (agent == null || agent.getSysUserId() == null || agent.getCommissionRate() == null)
            {
                continue;
            }
            BigDecimal rate = agent.getCommissionRate();
            ratesByUserId.put(agent.getSysUserId(), rate);
            addRate(ratesByAttributionKey, agent.getPartnerCustomerKey(), rate);
        }
        for (AgentSubId subId : agentSubIdMapper.selectSubIdListByUserIds(userIds))
        {
            if (subId != null && subId.getCreatedBy() != null)
            {
                addRate(ratesByAttributionKey, subId.getSubid(), ratesByUserId.get(subId.getCreatedBy()));
            }
        }
        return ratesByAttributionKey;
    }

    private void addRate(Map<String, BigDecimal> rates, String key, BigDecimal rate)
    {
        if (StringUtils.isNotBlank(key) && rate != null)
        {
            rates.put(key.trim().toLowerCase(java.util.Locale.ROOT), rate);
        }
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
