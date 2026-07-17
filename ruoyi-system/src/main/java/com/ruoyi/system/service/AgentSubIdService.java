package com.ruoyi.system.service;

import jakarta.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.AgentSubId;
import com.ruoyi.system.mapper.AgentSubIdMapper;

@Service
public class AgentSubIdService
{
    private static final String FIXED_PROMO_BASE_LINK = "https://getstartedtiktok.partnerlinks.io/vsj2hf8hifmg";

    private final AgentSubIdMapper agentSubIdMapper;
    private final JdbcTemplate jdbcTemplate;

    public AgentSubIdService(AgentSubIdMapper agentSubIdMapper, JdbcTemplate jdbcTemplate)
    {
        this.agentSubIdMapper = agentSubIdMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureSchema()
    {
        jdbcTemplate.execute(
                "create table if not exists agent_subid ("
                        + "id bigint(20) not null auto_increment comment '主键ID',"
                        + "subid varchar(50) not null comment 'SubId',"
                        + "name varchar(100) default null comment '名称',"
                        + "status tinyint(1) not null default 1 comment '状态',"
                        + "source varchar(100) default null comment '来源',"
                        + "created_by bigint(20) not null comment '拥有者用户ID',"
                        + "created_by_name varchar(30) not null comment '拥有者用户名',"
                        + "promo_link varchar(500) default null comment '推广链接',"
                        + "bound_at datetime not null comment '绑定时间',"
                        + "create_by varchar(64) default '' comment '创建者',"
                        + "create_time datetime comment '创建时间',"
                        + "update_by varchar(64) default '' comment '更新者',"
                        + "update_time datetime comment '更新时间',"
                        + "remark varchar(500) default null comment '备注',"
                        + "primary key (id),"
                        + "unique key uk_agent_subid_owner (created_by, subid),"
                        + "key idx_agent_subid_owner (created_by)"
                        + ") engine=innodb comment='代理SubId绑定表'");
        jdbcTemplate.update(
                "insert into sys_config(config_name, config_key, config_value, config_type, create_by, create_time, remark) "
                        + "select ?, ?, ?, ?, ?, sysdate(), ? from dual "
                        + "where not exists (select 1 from sys_config where config_key = ?)",
                "代理SubId推广基础链接",
                "agent.subid.base-link",
                FIXED_PROMO_BASE_LINK,
                "N",
                "admin",
                "配置后用于生成SubId推广链接，例如 https://getstartedtiktok.partnerlinks.io/xxxx",
                "agent.subid.base-link");
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
        return FIXED_PROMO_BASE_LINK;
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
