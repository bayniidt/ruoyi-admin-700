package com.ruoyi.system.mapper;

import java.util.List;
import java.util.Collection;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.AgentSubId;

public interface AgentSubIdMapper
{
    List<AgentSubId> selectMySubIdList(@Param("createdBy") Long createdBy);

    List<AgentSubId> selectSubIdListByUserIds(@Param("userIds") Collection<Long> userIds);

    List<String> selectSubIdsByUserIds(@Param("userIds") Collection<Long> userIds);

    int countBySubid(@Param("subid") String subid);

    int insertAgentSubId(AgentSubId subId);

    int updateSubIdOwner(@Param("id") Long id,
            @Param("targetUserId") Long targetUserId,
            @Param("targetUserName") String targetUserName,
            @Param("manageableOwnerUserIds") Collection<Long> manageableOwnerUserIds,
            @Param("updateBy") String updateBy);
}
