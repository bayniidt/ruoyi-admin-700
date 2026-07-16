package com.ruoyi.web.controller.agent;

import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.AgentClient;
import com.ruoyi.system.domain.AgentUsage;
import com.ruoyi.system.service.AgentClientService;

/**
 * 提供给下级客户独立系统调用的开放接口。
 */
@Anonymous
@RestController
@RequestMapping("/openapi/v1/agent")
public class AgentOpenApiController extends BaseController
{
    private final AgentClientService agentClientService;

    public AgentOpenApiController(AgentClientService agentClientService)
    {
        this.agentClientService = agentClientService;
    }

    @PostMapping("/usage")
    public AjaxResult reportUsage(@RequestHeader("X-Agent-Key") String apiKey,
            @RequestHeader("X-Agent-Secret") String apiSecret,
            @Valid @RequestBody AgentUsage usage)
    {
        AgentClient agent = agentClientService.authenticate(apiKey, apiSecret);
        boolean created = agentClientService.recordUsage(agent, usage);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requestId", usage.getRequestId());
        result.put("duplicate", !created);
        return AjaxResult.success(created ? "消耗数据上报成功" : "该 requestId 已上报，本次未重复计入", result);
    }
}
