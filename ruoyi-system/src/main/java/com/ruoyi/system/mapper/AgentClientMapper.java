package com.ruoyi.system.mapper;

import java.util.List;
import java.util.Collection;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.AgentClient;
import com.ruoyi.system.domain.AgentUsage;

public interface AgentClientMapper
{
    List<AgentClient> selectAgentList(@Param("visibleUserIds") Collection<Long> visibleUserIds,
            @Param("keyword") String keyword);

    List<AgentClient> selectAllAgentRelations();

    List<String> selectPartnerKeysByUserIds(@Param("userIds") Collection<Long> userIds);

    AgentClient selectAgentByApiKey(String apiKey);

    AgentClient selectAgentBySysUserId(Long sysUserId);

    Long selectAgentRoleId();

    int countByOwnerAndUserName(@Param("ownerUserId") Long ownerUserId, @Param("userName") String userName);

    int countByPartnerCustomerKey(@Param("partnerCustomerKey") String partnerCustomerKey);

    int insertAgent(AgentClient agent);

    int updateSecret(@Param("agentId") Long agentId, @Param("ownerUserId") Long ownerUserId,
            @Param("apiSecretHash") String apiSecretHash);

    int updateAgent(@Param("agent") AgentClient agent, @Param("ownerUserId") Long ownerUserId);

    int insertUsage(AgentUsage usage);
}
