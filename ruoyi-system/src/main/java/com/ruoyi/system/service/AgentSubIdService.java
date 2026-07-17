package com.ruoyi.system.service;

import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.AgentSubId;
import com.ruoyi.system.mapper.AgentSubIdMapper;

@Service
public class AgentSubIdService
{
    private final AgentSubIdMapper agentSubIdMapper;
    private final ISysUserService sysUserService;
    private final ISysConfigService sysConfigService;

    public AgentSubIdService(AgentSubIdMapper agentSubIdMapper, ISysUserService sysUserService,
            ISysConfigService sysConfigService)
    {
        this.agentSubIdMapper = agentSubIdMapper;
        this.sysUserService = sysUserService;
        this.sysConfigService = sysConfigService;
    }

    public List<AgentSubId> selectMySubIdList(Long userId)
    {
        List<AgentSubId> list = agentSubIdMapper.selectMySubIdList(userId);
        fillPromoLinks(list, userId);
        return list;
    }

    public List<AgentSubId> selectDownlineSubIdList(Long ownerUserId)
    {
        List<AgentSubId> list = agentSubIdMapper.selectDownlineSubIdList(ownerUserId);
        fillPromoLinks(list, ownerUserId);
        return list;
    }

    @Transactional
    public AgentSubId createSubId(String subid, Long userId, String username)
    {
        String normalizedSubid = StringUtils.trim(subid);
        if (StringUtils.isEmpty(normalizedSubid))
        {
            throw new ServiceException("SubId 值不能为空");
        }
        if (!normalizedSubid.matches("^[A-Za-z0-9_]+$"))
        {
            throw new ServiceException("仅支持字母、数字和下划线");
        }
        if (normalizedSubid.length() < 2 || normalizedSubid.length() > 50)
        {
            throw new ServiceException("长度必须介于 2 和 50 之间");
        }
        if (agentSubIdMapper.countBySubid(userId, normalizedSubid) > 0)
        {
            throw new ServiceException("该 SubId 已存在");
        }

        AgentSubId entity = new AgentSubId();
        entity.setSubid(normalizedSubid);
        entity.setStatus(1);
        entity.setCreatedBy(userId);
        entity.setCreatedByName(username);
        entity.setPromoLink(buildPromoLink(userId, normalizedSubid));
        entity.setBoundAt(new Date());
        entity.setCreateBy(username);
        agentSubIdMapper.insertAgentSubId(entity);
        return entity;
    }

    private String buildPromoLink(Long userId, String subid)
    {
        String baseLink = resolveBaseLink(userId);
        if (StringUtils.isEmpty(baseLink))
        {
            return "";
        }
        String separator = baseLink.contains("?") ? "&" : "?";
        return baseLink + separator + "sid=" + subid;
    }

    private String resolveBaseLink(Long userId)
    {
        String baseLink = StringUtils.trim(sysConfigService.selectConfigByKey("agent.subid.base-link"));
        if (StringUtils.isEmpty(baseLink))
        {
            SysUser user = sysUserService.selectUserById(userId);
            String userValue = user == null ? null : StringUtils.trim(user.getPartnerStackKey());
            if (looksLikeHttpUrl(userValue))
            {
                baseLink = userValue;
            }
        }
        return baseLink;
    }

    private boolean looksLikeHttpUrl(String value)
    {
        return StringUtils.isNotEmpty(value)
                && (value.startsWith("http://") || value.startsWith("https://"));
    }

    private void fillPromoLinks(List<AgentSubId> list, Long userId)
    {
        String baseLink = resolveBaseLink(userId);
        if (StringUtils.isEmpty(baseLink))
        {
            return;
        }
        for (AgentSubId item : list)
        {
            if (item != null && StringUtils.isEmpty(item.getPromoLink()) && StringUtils.isNotEmpty(item.getSubid()))
            {
                String separator = baseLink.contains("?") ? "&" : "?";
                item.setPromoLink(baseLink + separator + "sid=" + item.getSubid());
            }
        }
    }
}
