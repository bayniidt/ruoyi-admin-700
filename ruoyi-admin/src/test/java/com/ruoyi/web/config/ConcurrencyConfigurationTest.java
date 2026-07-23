package com.ruoyi.web.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class ConcurrencyConfigurationTest
{
    @Test
    void keepsTomcatConcurrencyBoundedForSmallServer() throws IOException
    {
        PropertySource<?> properties = load("application.yml");

        assertEquals("1000", properties.getProperty("server.tomcat.max-connections").toString());
        assertEquals("1000", properties.getProperty("server.tomcat.accept-count").toString());
        assertEquals("200", properties.getProperty("server.tomcat.threads.max").toString());
        assertEquals("20", properties.getProperty("server.tomcat.threads.min-spare").toString());
        assertEquals("20s", properties.getProperty("server.tomcat.connection-timeout").toString());
        assertEquals("20s", properties.getProperty("server.tomcat.keep-alive-timeout").toString());
    }

    @Test
    void leavesEnoughConnectionsForSharedRedisAndDatabasePools() throws IOException
    {
        PropertySource<?> application = load("application.yml");
        PropertySource<?> druid = load("application-druid.yml");

        assertEquals("32", application.getProperty("spring.data.redis.lettuce.pool.max-active").toString());
        assertEquals("2s", application.getProperty("spring.data.redis.lettuce.pool.max-wait").toString());
        assertEquals("32", druid.getProperty("spring.datasource.druid.maxActive").toString());
        assertEquals("5000", druid.getProperty("spring.datasource.druid.maxWait").toString());
    }

    private PropertySource<?> load(String resourceName) throws IOException
    {
        return new YamlPropertySourceLoader().load(resourceName, new ClassPathResource(resourceName)).get(0);
    }
}
