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
import com.ruoyi.system.domain.AgentClient;
import com.ruoyi.system.domain.AgentUsage;

class AgentClientMapperXmlTest
{
    @Test
    void emptyNonAdminScopeGeneratesAlwaysFalseConditionInsteadOfInvalidInClause() throws Exception
    {
        BoundSql boundSql = selectAgentListSql(Collections.emptySet());
        String sql = normalize(boundSql.getSql());

        assertTrue(sql.contains("WHERE 1 = 0"));
        assertFalse(sql.contains("sys_user_id in group by"));
    }

    @Test
    void nullAdminScopeDoesNotRestrictAgentRows() throws Exception
    {
        String sql = normalize(selectAgentListSql(null).getSql());

        assertFalse(sql.contains("WHERE 1 = 0"));
        assertFalse(sql.contains("sys_user_id in"));
    }

    private BoundSql selectAgentListSql(Object visibleUserIds) throws Exception
    {
        Configuration configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias("AgentClient", AgentClient.class);
        configuration.getTypeAliasRegistry().registerAlias("AgentUsage", AgentUsage.class);
        String resource = "mapper/system/AgentClientMapper.xml";
        try (Reader reader = Resources.getResourceAsReader(resource))
        {
            new XMLMapperBuilder(reader, configuration, resource, configuration.getSqlFragments()).parse();
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("visibleUserIds", visibleUserIds);
        parameters.put("keyword", null);
        return configuration.getMappedStatement(
                "com.ruoyi.system.mapper.AgentClientMapper.selectAgentList").getBoundSql(parameters);
    }

    private String normalize(String sql)
    {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
