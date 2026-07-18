package com.ruoyi.web.controller.agent;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.AgentClient;
import com.ruoyi.system.service.AgentClientService;
import com.ruoyi.system.service.AgentDataScopeService;

/**
 * 下级代理管理。
 */
@RestController
@RequestMapping("/agent/manage")
public class AgentManageController extends BaseController
{
    private final AgentClientService agentClientService;
    private final AgentDataScopeService agentDataScopeService;

    public AgentManageController(AgentClientService agentClientService, AgentDataScopeService agentDataScopeService)
    {
        this.agentClientService = agentClientService;
        this.agentDataScopeService = agentDataScopeService;
    }

    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String keyword)
    {
        Set<Long> visibleUserIds = resolveManagedAgentUserIds(SecurityUtils.isAdmin(), getUserId());
        startPage();
        List<AgentClient> list = agentClientService.selectAgentList(visibleUserIds, keyword);
        return getDataTable(list);
    }

    Set<Long> resolveManagedAgentUserIds(boolean admin, Long currentUserId)
    {
        return admin ? null : agentDataScopeService.selectDescendantUserIds(currentUserId);
    }

    @Log(title = "代理管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AgentClient agent)
    {
        Map<String, Object> credential = agentClientService.createAgent(agent, getUserId(),
                getLoginUser().getDeptId(), getUsername());
        return AjaxResult.success("新增代理成功，请立即保存接口密钥", credential);
    }

    @Log(title = "代理管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{agentId}")
    public AjaxResult update(@PathVariable Long agentId, @RequestBody AgentClient agent)
    {
        agent.setAgentId(agentId);
        agentClientService.updateAgent(agent, SecurityUtils.isAdmin() ? null : getUserId());
        return success("代理信息更新成功");
    }

    @Log(title = "代理接口密钥", businessType = BusinessType.UPDATE)
    @PostMapping("/{agentId}/reset-secret")
    public AjaxResult resetSecret(@PathVariable Long agentId)
    {
        String apiSecret = agentClientService.resetSecret(agentId, SecurityUtils.isAdmin() ? null : getUserId());
        return AjaxResult.success("密钥已重置，请立即保存", Map.of("apiSecret", apiSecret));
    }
}
