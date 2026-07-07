package ai.toolkit.mcp.tool;

import ai.toolkit.mcp.McpToolkitApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = McpToolkitApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ExplainQueryPlanToolTest {

    @Autowired
    ExplainQueryPlanTool explainQueryPlanTool;

    @Test
    public void testExplainQueryPlanEmptyQuery() {
        ExplainQueryPlanTool.QueryPlan plan = explainQueryPlanTool.explainQueryPlan("");
        assertThat(plan.error).isNotNull();
        assertThat(plan.error).contains("cannot be empty");
    }

    @Test
    public void testExplainQueryPlanNullQuery() {
        ExplainQueryPlanTool.QueryPlan plan = explainQueryPlanTool.explainQueryPlan(null);
        assertThat(plan.error).isNotNull();
    }

    @Test
    public void testExplainQueryPlanInvalidQuery() {
        ExplainQueryPlanTool.QueryPlan plan = explainQueryPlanTool.explainQueryPlan("INVALID SQL");
        assertThat(plan.error).isNotNull();
    }

    @Test
    public void testExplainQueryPlanInsertRejected() {
        ExplainQueryPlanTool.QueryPlan plan = explainQueryPlanTool.explainQueryPlan("INSERT INTO customer VALUES (1, 'test')");
        assertThat(plan.error).isNotNull();
        assertThat(plan.error).contains("SELECT");
    }

    @Test
    public void testExplainQueryPlanSelectValid() {
        ExplainQueryPlanTool.QueryPlan plan = explainQueryPlanTool.explainQueryPlan("SELECT * FROM customer WHERE id = 1");

        assertThat(plan.sql).isNotNull();
        assertThat(plan.dialect).isNotNull();
        assertThat(plan.planRows).isNotNull();
    }

}

