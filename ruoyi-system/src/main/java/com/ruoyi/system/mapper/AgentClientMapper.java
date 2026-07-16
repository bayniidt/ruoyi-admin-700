package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.AgentClient;
import com.ruoyi.system.domain.AgentUsage;

public interface AgentClientMapper
{
    List<AgentClient> selectAgentList(@Param("ownerUserId") Long ownerUserId, @Param("keyword") String keyword);

    AgentClient selectAgentByApiKey(String apiKey);

    AgentClient selectAgentBySysUserId(Long sysUserId);

    Long selectAgentRoleId();

    int countByOwnerAndUserName(@Param("ownerUserId") Long ownerUserId, @Param("userName") String userName);

    int insertAgent(AgentClient agent);

    int updateSecret(@Param("agentId") Long agentId, @Param("ownerUserId") Long ownerUserId,
            @Param("apiSecretHash") String apiSecretHash);

    int insertUsage(AgentUsage usage);
}
