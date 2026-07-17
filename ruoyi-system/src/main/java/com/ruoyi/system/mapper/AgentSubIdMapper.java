package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.AgentSubId;

public interface AgentSubIdMapper
{
    List<AgentSubId> selectMySubIdList(@Param("createdBy") Long createdBy);

    List<AgentSubId> selectDownlineSubIdList(@Param("ownerUserId") Long ownerUserId);

    int countBySubid(@Param("createdBy") Long createdBy, @Param("subid") String subid);

    int insertAgentSubId(AgentSubId subId);
}
