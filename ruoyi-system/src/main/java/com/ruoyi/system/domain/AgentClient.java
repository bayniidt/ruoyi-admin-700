package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 下级代理及其开放接口凭证。
 */
public class AgentClient extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long agentId;
    private Long ownerUserId;
    private Long sysUserId;
    private String userName;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    @JsonIgnore
    private String passwordHash;
    private String nickName;
    private String phonenumber;
    private String email;
    private BigDecimal commissionRate;
    private String partnerCustomerKey;
    private String apiKey;
    @JsonIgnore
    private String apiSecretHash;
    private String status;
    private Long usageCount;
    private Long dataCount;
    private BigDecimal totalSpend;
    private Date lastReportTime;

    public Long getAgentId() { return agentId; }
    public void setAgentId(Long agentId) { this.agentId = agentId; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public Long getSysUserId() { return sysUserId; }
    public void setSysUserId(Long sysUserId) { this.sysUserId = sysUserId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }
    public String getPhonenumber() { return phonenumber; }
    public void setPhonenumber(String phonenumber) { this.phonenumber = phonenumber; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }
    public String getPartnerCustomerKey() { return partnerCustomerKey; }
    public void setPartnerCustomerKey(String partnerCustomerKey) { this.partnerCustomerKey = partnerCustomerKey; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getApiSecretHash() { return apiSecretHash; }
    public void setApiSecretHash(String apiSecretHash) { this.apiSecretHash = apiSecretHash; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getUsageCount() { return usageCount; }
    public void setUsageCount(Long usageCount) { this.usageCount = usageCount; }
    public Long getDataCount() { return dataCount; }
    public void setDataCount(Long dataCount) { this.dataCount = dataCount; }
    public BigDecimal getTotalSpend() { return totalSpend; }
    public void setTotalSpend(BigDecimal totalSpend) { this.totalSpend = totalSpend; }
    public Date getLastReportTime() { return lastReportTime; }
    public void setLastReportTime(Date lastReportTime) { this.lastReportTime = lastReportTime; }
}
