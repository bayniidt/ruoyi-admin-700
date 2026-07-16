package com.ruoyi.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * PartnerStack API 配置
 */
@Component
@ConfigurationProperties(prefix = "partnerstack")
public class PartnerStackConfig
{
    private String baseUrl = "https://api.partnerstack.com/api/v2";

    private String token;

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }

    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
    }
}
