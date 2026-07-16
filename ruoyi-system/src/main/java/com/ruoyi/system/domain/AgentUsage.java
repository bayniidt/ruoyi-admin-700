package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 下级系统上报的消耗记录。
 */
public class AgentUsage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long usageId;
    private Long agentId;
    @NotBlank(message = "requestId 不能为空")
    @Size(max = 64, message = "requestId 不能超过64个字符")
    private String requestId;
    @NotBlank(message = "metric 不能为空")
    @Size(max = 50, message = "metric 不能超过50个字符")
    private String metric;
    @Min(value = 0, message = "dataCount 不能小于0")
    private Long dataCount;
    @DecimalMin(value = "0", message = "spend 不能小于0")
    private BigDecimal spend;
    @Size(max = 10, message = "currency 不能超过10个字符")
    private String currency;
    @Size(max = 100, message = "customerKey 不能超过100个字符")
    private String customerKey;
    private Date reportedAt;

    public Long getUsageId() { return usageId; }
    public void setUsageId(Long usageId) { this.usageId = usageId; }
    public Long getAgentId() { return agentId; }
    public void setAgentId(Long agentId) { this.agentId = agentId; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }
    public Long getDataCount() { return dataCount; }
    public void setDataCount(Long dataCount) { this.dataCount = dataCount; }
    public BigDecimal getSpend() { return spend; }
    public void setSpend(BigDecimal spend) { this.spend = spend; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getCustomerKey() { return customerKey; }
    public void setCustomerKey(String customerKey) { this.customerKey = customerKey; }
    public Date getReportedAt() { return reportedAt; }
    public void setReportedAt(Date reportedAt) { this.reportedAt = reportedAt; }
}
