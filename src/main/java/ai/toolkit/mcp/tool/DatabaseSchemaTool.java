package ai.toolkit.mcp.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Component
public class DatabaseSchemaTool {

    private final DataSource dataSource;

    public DatabaseSchemaTool(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Tool(name = "get_db_schema", description = "Returns database schema metadata (tables, columns, PKs, FKs). Optional parameter tableNameFilter supports SQL pattern (e.g. CUSTOMER%). Uses DatabaseMetaData only.")
    public DbSchemaResult getDbSchema(String tableNameFilter) throws SQLException {
        List<TableInfo> tables = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String[] types = {"TABLE"};
            String tablePattern = tableNameFilter == null ? "%" : tableNameFilter;

            try (ResultSet rs = meta.getTables(conn.getCatalog(), null, tablePattern, types)) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    TableInfo ti = new TableInfo();
                    ti.name = tableName;
                    ti.columns = new ArrayList<>();

                    // columns
                    try (ResultSet cols = meta.getColumns(conn.getCatalog(), null, tableName, null)) {
                        while (cols.next()) {
                            ColumnInfo ci = new ColumnInfo();
                            ci.name = cols.getString("COLUMN_NAME");
                            ci.type = cols.getString("TYPE_NAME");
                            ci.nullable = DatabaseMetaData.columnNullable == cols.getInt("NULLABLE");
                            ci.size = cols.getInt("COLUMN_SIZE");
                            ti.columns.add(ci);
                        }
                    }

                    // primary keys
                    ti.primaryKeys = new ArrayList<>();
                    try (ResultSet pks = meta.getPrimaryKeys(conn.getCatalog(), null, tableName)) {
                        while (pks.next()) {
                            ti.primaryKeys.add(pks.getString("COLUMN_NAME"));
                        }
                    }

                    // foreign keys (imported keys)
                    ti.foreignKeys = new ArrayList<>();
                    try (ResultSet fks = meta.getImportedKeys(conn.getCatalog(), null, tableName)) {
                        while (fks.next()) {
                            ForeignKeyInfo fk = new ForeignKeyInfo();
                            fk.fkColumn = fks.getString("FKCOLUMN_NAME");
                            fk.pkTable = fks.getString("PKTABLE_NAME");
                            fk.pkColumn = fks.getString("PKCOLUMN_NAME");
                            fk.fkName = fks.getString("FK_NAME");
                            ti.foreignKeys.add(fk);
                        }
                    }

                    tables.add(ti);
                }
            }
        }

        DbSchemaResult res = new DbSchemaResult();
        res.tables = tables;
        return res;
    }

    public static class DbSchemaResult {
        public List<TableInfo> tables;
    }

    public static class TableInfo {
        public String name;
        public List<ColumnInfo> columns;
        public List<String> primaryKeys;
        public List<ForeignKeyInfo> foreignKeys;
    }

    public static class ColumnInfo {
        public String name;
        public String type;
        public boolean nullable;
        public Integer size;
    }

    public static class ForeignKeyInfo {
        public String fkName;
        public String fkColumn;
        public String pkTable;
        public String pkColumn;
    }

}

