package com.ruoyi.web.controller.agent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.AgentSubId;
import com.ruoyi.system.domain.AgentClient;
import com.ruoyi.system.service.AgentClientService;
import com.ruoyi.system.service.AgentSubIdService;
import com.ruoyi.system.service.AgentDataScopeService;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.common.utils.SecurityUtils;

@RestController
@RequestMapping("/agent/subid")
public class AgentSubIdController extends BaseController
{
    private final AgentSubIdService agentSubIdService;
    private final AgentDataScopeService agentDataScopeService;
    private final AgentClientService agentClientService;
    private final ISysUserService userService;

    public AgentSubIdController(AgentSubIdService agentSubIdService, AgentDataScopeService agentDataScopeService,
            AgentClientService agentClientService, ISysUserService userService)
    {
        this.agentSubIdService = agentSubIdService;
        this.agentDataScopeService = agentDataScopeService;
        this.agentClientService = agentClientService;
        this.userService = userService;
    }

    @GetMapping("/list")
    public TableDataInfo list()
    {
        List<AgentSubId> list = agentSubIdService.selectMySubIdList(getUserId());
        return getDataTable(list);
    }

    @GetMapping("/downline")
    public TableDataInfo downline()
    {
        Set<Long> userIds;
        if (SecurityUtils.isAdmin())
        {
            userIds = agentDataScopeService.selectAllAgentUserIds();
        }
        else
        {
            userIds = agentDataScopeService.selectDescendantUserIds(getUserId());
        }
        List<AgentSubId> list = agentSubIdService.selectSubIdListByUserIds(userIds, getUserId());
        return getDataTable(list);
    }

    @Log(title = "SubId管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Map<String, String> body)
    {
        AgentSubId subId = agentSubIdService.createSubId(body.get("subId"), getUserId(), getUsername());
        return AjaxResult.success("SubId 创建成功", subId);
    }

    @GetMapping("/assignable-owners")
    public AjaxResult assignableOwners()
    {
        boolean admin = SecurityUtils.isAdmin();
        Set<Long> targetUserIds = resolveAssignableOwnerUserIds(admin, getUserId());
        List<AgentClient> agents = agentClientService.selectAgentList(targetUserIds, null);
        List<Map<String, Object>> owners = new ArrayList<>();
        if (admin)
        {
            Map<String, Object> current = new LinkedHashMap<>();
            current.put("userId", getUserId());
            current.put("userName", getUsername());
            current.put("nickName", getLoginUser().getUser().getNickName());
            owners.add(current);
        }
        for (AgentClient agent : agents)
        {
            Map<String, Object> owner = new LinkedHashMap<>();
            owner.put("userId", agent.getSysUserId());
            owner.put("userName", agent.getUserName());
            owner.put("nickName", agent.getNickName());
            owners.add(owner);
        }
        return AjaxResult.success(owners);
    }

    @Log(title = "SubId拥有者分配", businessType = BusinessType.UPDATE)
    @PutMapping("/{subIdRecordId}/owner")
    public AjaxResult assignOwner(@PathVariable Long subIdRecordId, @RequestBody Map<String, Long> body)
    {
        Long targetUserId = body.get("ownerUserId");
        boolean admin = SecurityUtils.isAdmin();
        Set<Long> targetUserIds = resolveAssignableOwnerUserIds(admin, getUserId());
        if (targetUserId == null || !targetUserIds.contains(targetUserId))
        {
            return error("只能分配给授权范围内的下级账号");
        }
        SysUser targetUser = userService.selectUserById(targetUserId);
        if (targetUser == null || !"0".equals(targetUser.getStatus()))
        {
            return error("目标账号不存在或已停用");
        }
        agentSubIdService.assignOwner(subIdRecordId, targetUserId, targetUser.getUserName(),
                resolveManageableOwnerUserIds(admin, getUserId()), getUsername());
        return success("SubId 拥有者分配成功");
    }

    Set<Long> resolveAssignableOwnerUserIds(boolean admin, Long currentUserId)
    {
        if (!admin)
        {
            return agentDataScopeService.selectDescendantUserIds(currentUserId);
        }
        Set<Long> userIds = new HashSet<>(agentDataScopeService.selectAllAgentUserIds());
        userIds.add(currentUserId);
        return userIds;
    }

    Set<Long> resolveManageableOwnerUserIds(boolean admin, Long currentUserId)
    {
        return admin ? null : agentDataScopeService.selectSelfAndDescendantUserIds(currentUserId);
    }
}
