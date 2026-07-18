package com.ruoyi.system.service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.AgentClient;
import com.ruoyi.system.domain.AgentUsage;
import com.ruoyi.system.mapper.AgentClientMapper;
import com.ruoyi.system.service.ISysUserService;

@Service
public class AgentClientService
{
    private static final BigDecimal MAX_COMMISSION_RATE = new BigDecimal("100");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AgentClientMapper agentClientMapper;
    private final ISysUserService sysUserService;

    public AgentClientService(AgentClientMapper agentClientMapper, ISysUserService sysUserService)
    {
        this.agentClientMapper = agentClientMapper;
        this.sysUserService = sysUserService;
    }

    public List<AgentClient> selectAgentList(Collection<Long> visibleUserIds, String keyword)
    {
        return agentClientMapper.selectAgentList(visibleUserIds, StringUtils.trim(keyword));
    }

    @Transactional
    public Map<String, Object> createAgent(AgentClient agent, Long ownerUserId, Long ownerDeptId, String createBy)
    {
        validateAgent(agent);
        if (agentClientMapper.countByOwnerAndUserName(ownerUserId, agent.getUserName()) > 0)
        {
            throw new ServiceException("代理用户名已存在");
        }

        SysUser existingUser = sysUserService.selectUserByUserName(agent.getUserName().trim());
        if (existingUser != null)
        {
            throw new ServiceException("登录账号已存在");
        }
        Long agentRoleId = agentClientMapper.selectAgentRoleId();
        if (agentRoleId == null)
        {
            throw new ServiceException("代理角色未初始化，请先执行 sql/agent_api.sql");
        }

        String apiSecret = randomToken(32);
        String passwordHash = SecurityUtils.encryptPassword(agent.getPassword() == null ? "" : agent.getPassword());
        String partnerCustomerKey = StringUtils.isBlank(agent.getPartnerCustomerKey())
                ? agent.getUserName().trim() : agent.getPartnerCustomerKey().trim();
        if (agentClientMapper.countByPartnerCustomerKey(partnerCustomerKey) > 0)
        {
            throw new ServiceException("PartnerStack 数据Key已被其他代理绑定");
        }
        SysUser sysUser = new SysUser();
        sysUser.setDeptId(ownerDeptId);
        sysUser.setUserName(agent.getUserName().trim());
        sysUser.setNickName(agent.getNickName().trim());
        sysUser.setEmail(agent.getEmail());
        sysUser.setPhonenumber(agent.getPhonenumber());
        sysUser.setPassword(passwordHash);
        sysUser.setStatus("0");
        sysUser.setPartnerStackKey(partnerCustomerKey);
        sysUser.setRoleIds(new Long[] { agentRoleId });
        sysUser.setCreateBy(createBy);
        sysUser.setRemark(agent.getRemark());
        sysUserService.insertUser(sysUser);

        agent.setOwnerUserId(ownerUserId);
        agent.setSysUserId(sysUser.getUserId());
        agent.setUserName(agent.getUserName().trim());
        agent.setNickName(agent.getNickName().trim());
        agent.setPasswordHash(passwordHash);
        agent.setPassword(null);
        agent.setPartnerCustomerKey(partnerCustomerKey);
        agent.setApiKey("ag_" + randomToken(18));
        agent.setApiSecretHash(SecurityUtils.encryptPassword(apiSecret));
        agent.setStatus("0");
        agent.setCreateBy(createBy);
        agentClientMapper.insertAgent(agent);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("agentId", agent.getAgentId());
        result.put("loginUserName", agent.getUserName());
        result.put("apiKey", agent.getApiKey());
        result.put("apiSecret", apiSecret);
        return result;
    }

    public AgentClient selectAgentBySysUserId(Long sysUserId)
    {
        return agentClientMapper.selectAgentBySysUserId(sysUserId);
    }

    @Transactional
    public String resetSecret(Long agentId, Long ownerUserId)
    {
        String apiSecret = randomToken(32);
        int rows = agentClientMapper.updateSecret(agentId, ownerUserId, SecurityUtils.encryptPassword(apiSecret));
        if (rows == 0)
        {
            throw new ServiceException("代理不存在或无权操作");
        }
        return apiSecret;
    }

    @Transactional
    public void updateAgent(AgentClient agent, Long ownerUserId)
    {
        if (agent == null || agent.getAgentId() == null)
        {
            throw new ServiceException("代理不存在");
        }
        if (agent.getCommissionRate() != null
                && (agent.getCommissionRate().compareTo(BigDecimal.ZERO) < 0
                || agent.getCommissionRate().compareTo(MAX_COMMISSION_RATE) > 0))
        {
            throw new ServiceException("佣金比例必须介于0和20之间");
        }
        if (agentClientMapper.updateAgent(agent, ownerUserId) == 0)
        {
            throw new ServiceException("代理不存在或无权操作");
        }
    }

    public AgentClient authenticate(String apiKey, String apiSecret)
    {
        if (StringUtils.isBlank(apiKey) || StringUtils.isBlank(apiSecret))
        {
            throw new ServiceException("缺少 X-Agent-Key 或 X-Agent-Secret", HttpStatus.UNAUTHORIZED);
        }
        AgentClient agent = agentClientMapper.selectAgentByApiKey(apiKey.trim());
        if (agent == null || !"0".equals(agent.getStatus())
                || !SecurityUtils.matchesPassword(apiSecret, agent.getApiSecretHash()))
        {
            throw new ServiceException("接口凭证无效或已停用", HttpStatus.UNAUTHORIZED);
        }
        return agent;
    }

    @Transactional
    public boolean recordUsage(AgentClient agent, AgentUsage usage)
    {
        usage.setAgentId(agent.getAgentId());
        usage.setDataCount(usage.getDataCount() == null ? 0L : usage.getDataCount());
        usage.setSpend(usage.getSpend() == null ? BigDecimal.ZERO : usage.getSpend());
        usage.setCurrency(StringUtils.isBlank(usage.getCurrency()) ? "USD" : usage.getCurrency().trim().toUpperCase());
        usage.setReportedAt(usage.getReportedAt() == null ? new Date() : usage.getReportedAt());
        return agentClientMapper.insertUsage(usage) > 0;
    }

    private void validateAgent(AgentClient agent)
    {
        if (agent == null || StringUtils.isBlank(agent.getUserName()))
        {
            throw new ServiceException("用户名不能为空");
        }
        if (agent.getUserName().trim().length() < 2 || agent.getUserName().trim().length() > 20)
        {
            throw new ServiceException("用户名长度必须介于2和20之间");
        }
        if (StringUtils.isBlank(agent.getPassword()))
        {
            throw new ServiceException("用户密码不能为空");
        }
        if (StringUtils.isBlank(agent.getNickName()))
        {
            throw new ServiceException("用户昵称不能为空");
        }
        if (agent.getCommissionRate() == null || agent.getCommissionRate().compareTo(BigDecimal.ZERO) < 0
                || agent.getCommissionRate().compareTo(MAX_COMMISSION_RATE) > 0)
        {
            throw new ServiceException("佣金比例必须介于0和20之间");
        }
    }

    private String randomToken(int byteLength)
    {
        byte[] bytes = new byte[byteLength];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
