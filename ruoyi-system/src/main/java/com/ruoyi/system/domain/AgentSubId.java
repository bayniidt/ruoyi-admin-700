package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

public class AgentSubId extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String subid;
    private String name;
    private Integer status;
    private String source;
    private Long createdBy;
    private String createdByName;
    private String promoLink;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date boundAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSubid() { return subid; }
    public void setSubid(String subid) { this.subid = subid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    public String getPromoLink() { return promoLink; }
    public void setPromoLink(String promoLink) { this.promoLink = promoLink; }
    public Date getBoundAt() { return boundAt; }
    public void setBoundAt(Date boundAt) { this.boundAt = boundAt; }
}
