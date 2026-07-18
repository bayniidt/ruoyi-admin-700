package com.ruoyi.system.mapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import com.ruoyi.system.domain.AgentSubId;

class AgentSubIdMapperXmlTest
{
    @Test
    void emptyManagementScopeCannotUpdateAnySubId() throws Exception
    {
        String sql = normalize(updateOwnerSql(Collections.emptySet()).getSql());

        assertTrue(sql.contains("and 1 = 0"));
    }

    @Test
    void nullAdminScopeDoesNotRestrictCurrentOwner() throws Exception
    {
        String sql = normalize(updateOwnerSql(null).getSql());

        assertFalse(sql.contains("created_by in"));
        assertFalse(sql.contains("and 1 = 0"));
    }

    private BoundSql updateOwnerSql(Object manageableOwnerUserIds) throws Exception
    {
        Configuration configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias("AgentSubId", AgentSubId.class);
        String resource = "mapper/system/AgentSubIdMapper.xml";
        try (Reader reader = Resources.getResourceAsReader(resource))
        {
            new XMLMapperBuilder(reader, configuration, resource, configuration.getSqlFragments()).parse();
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", 1L);
        parameters.put("targetUserId", 2L);
        parameters.put("targetUserName", "agent");
        parameters.put("manageableOwnerUserIds", manageableOwnerUserIds);
        parameters.put("updateBy", "admin");
        return configuration.getMappedStatement(
                "com.ruoyi.system.mapper.AgentSubIdMapper.updateSubIdOwner").getBoundSql(parameters);
    }

    private String normalize(String sql)
    {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
