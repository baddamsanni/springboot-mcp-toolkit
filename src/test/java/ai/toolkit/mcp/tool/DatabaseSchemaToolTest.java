package ai.toolkit.mcp.tool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ai.toolkit.mcp.McpToolkitApplication;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = McpToolkitApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DatabaseSchemaToolTest {

    @Autowired
    DatabaseSchemaTool schemaTool;

    @Test
    public void testSchemaIntrospection() throws SQLException {
        DatabaseSchemaTool.DbSchemaResult res = schemaTool.getDbSchema(null);
        assertThat(res).isNotNull();
        List<DatabaseSchemaTool.TableInfo> tables = res.tables;
        assertThat(tables).isNotEmpty();

        boolean hasCustomer = tables.stream().anyMatch(t -> t.name.equalsIgnoreCase("customer"));
        boolean hasOrder = tables.stream().anyMatch(t -> t.name.equalsIgnoreCase("order_record"));
        assertThat(hasCustomer).isTrue();
        assertThat(hasOrder).isTrue();

        DatabaseSchemaTool.TableInfo customer = tables.stream().filter(t -> t.name.equalsIgnoreCase("customer")).findFirst().orElse(null);
        assertThat(customer).isNotNull();
        assertThat(customer.columns).extracting(c -> c.name.toLowerCase(Locale.ROOT)).contains("id", "name", "email");

        DatabaseSchemaTool.TableInfo order = tables.stream().filter(t -> t.name.equalsIgnoreCase("order_record")).findFirst().orElse(null);
        assertThat(order).isNotNull();
        assertThat(order.foreignKeys).isNotEmpty();
        assertThat(order.foreignKeys.get(0).pkTable).isEqualToIgnoringCase("customer");
    }

}

