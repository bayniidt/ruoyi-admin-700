package com.ruoyi.web.controller.agent;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.AgentSubId;
import com.ruoyi.system.service.AgentSubIdService;

@RestController
@RequestMapping("/agent/subid")
public class AgentSubIdController extends BaseController
{
    private final AgentSubIdService agentSubIdService;

    public AgentSubIdController(AgentSubIdService agentSubIdService)
    {
        this.agentSubIdService = agentSubIdService;
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
        List<AgentSubId> list = agentSubIdService.selectDownlineSubIdList(getUserId());
        return getDataTable(list);
    }

    @Log(title = "SubId管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Map<String, String> body)
    {
        AgentSubId subId = agentSubIdService.createSubId(body.get("subId"), getUserId(), getUsername());
        return AjaxResult.success("SubId 创建成功", subId);
    }
}
