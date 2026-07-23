package com.ruoyi.web.controller.tutorial;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import com.ruoyi.common.utils.file.FileUploadUtils;

class TutorialFileUploadLimitTest
{
    @Test
    void doesNotDefineAnUploadSizeLimit()
    {
        assertEquals(Long.MAX_VALUE, FileUploadUtils.DEFAULT_MAX_SIZE);
    }

    @Test
    void configuresSpringMultipartUploadWithoutASizeLimit() throws IOException
    {
        var propertySource = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"))
                .get(0);

        assertEquals("-1", propertySource.getProperty("spring.servlet.multipart.max-file-size").toString());
        assertEquals("-1", propertySource.getProperty("spring.servlet.multipart.max-request-size").toString());
    }
}
