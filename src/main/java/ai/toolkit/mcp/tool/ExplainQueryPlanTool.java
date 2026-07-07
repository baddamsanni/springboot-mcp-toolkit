package ai.toolkit.mcp.tool;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import javax.sql.DataSource;

@Component
public class ExplainQueryPlanTool {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public ExplainQueryPlanTool(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Tool(name = "explain_query_plan", description = "Analyzes SQL query execution plan. Validates SELECT queries using JSqlParser and runs EXPLAIN. Returns plan details and estimated cost. Strictly read-only.")
    public QueryPlan explainQueryPlan(String sql) {
        QueryPlan plan = new QueryPlan();
        plan.sql = sql;
        plan.planRows = new ArrayList<>();

        if (sql == null || sql.trim().isEmpty()) {
            plan.error = "SQL query cannot be empty";
            return plan;
        }

        // Validate that it's a SELECT statement
        try {
            var statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select)) {
                plan.error = "Only SELECT statements are allowed. Query must be read-only.";
                return plan;
            }
        } catch (JSQLParserException e) {
            plan.error = "Invalid SQL syntax: " + e.getMessage();
            return plan;
        }

        // Get database dialect
        try {
            Connection conn = dataSource.getConnection();
            DatabaseMetaData meta = conn.getMetaData();
            plan.dialect = meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion();
            conn.close();
        } catch (SQLException e) {
            plan.dialect = "Unknown";
        }

        // Build EXPLAIN query based on dialect
        String explainQuery = buildExplainQuery(sql, plan.dialect);

        // Execute EXPLAIN
        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(explainQuery);
            for (Map<String, Object> row : results) {
                plan.planRows.add(row);
            }

            if (!results.isEmpty()) {
                // Try to estimate cost from first row if available
                Map<String, Object> firstRow = results.get(0);
                if (firstRow.containsKey("COST")) {
                    Object costObj = firstRow.get("COST");
                    if (costObj instanceof Number) {
                        plan.estimatedCost = ((Number) costObj).doubleValue();
                    }
                }
            }

        } catch (Exception e) {
            plan.error = "Failed to execute EXPLAIN: " + e.getMessage();
        }

        return plan;
    }

    private String buildExplainQuery(String sql, String dialect) {
        // Normalize dialect
        String normalized = dialect.toLowerCase();

        if (normalized.contains("postgresql")) {
            return "EXPLAIN ANALYZE " + sql;
        } else if (normalized.contains("mysql")) {
            return "EXPLAIN " + sql;
        } else if (normalized.contains("h2")) {
            return "EXPLAIN " + sql;
        } else if (normalized.contains("oracle")) {
            return "EXPLAIN PLAN FOR " + sql;
        } else {
            return "EXPLAIN " + sql;
        }
    }

    public static class QueryPlan {
        public String sql;
        public String dialect;
        public List<Map<String, Object>> planRows;
        public Double estimatedCost;
        public String error;
    }

}

