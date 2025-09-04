package com.example.database_migration_app.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Service
public class MysqlService {

    public static class Catalog {
        public Map<String, Table> tables = new LinkedHashMap<>();
    }

    public static class Table {
        public String schema, name;
        public List<Column> columns = new ArrayList<>();
        public List<String> pk = new ArrayList<>();
        public List<Index> indexes = new ArrayList<>();
    }

    public static class Column {
        public String name, mysqlType;
        public int size, scale;
        public boolean nullable;
        public String defaultVal;
    }

    public static class Index {
        public String name;
        public boolean unique;
        public List<String> columns = new ArrayList<>();
    }

    private final DataSource mysqlDataSource;

    private final JdbcTemplate mysqlJdbc;

    public MysqlService(DataSource mysqlDataSource, JdbcTemplate mysqlJdbc) {
        this.mysqlDataSource = mysqlDataSource;
        this.mysqlJdbc = mysqlJdbc;
    }

    public void readMysqlData() {
        Catalog catalog = new Catalog();
        try (Connection connection = mysqlDataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<String> schemas = new ArrayList<>();
            try (ResultSet databases = metaData.getCatalogs()) {
                while (databases.next()) {
                    String s = databases.getString("TABLE_CAT");
                    if (s == null) {
                        continue;
                    }
                    schemas.add(s);
                }
            }
            for (String schema : schemas) {
                try (ResultSet tables = metaData.getTables(schema, schema, "%", new String[]{"TABLE"})) {
                    while (tables.next()) {
                        String tableName = tables.getString("TABLE_NAME");

                        Table table = new Table();
                        table.schema = schema;
                        table.name = tableName;

                        // Columns
                        try (ResultSet columns = metaData.getColumns(schema, schema, tableName, "%")) {
                            while (columns.next()) {
                                Column col = new Column();
                                col.name = columns.getString("COLUMN_NAME");
                                col.mysqlType = columns.getString("TYPE_NAME");
                                col.size = columns.getInt("COLUMN_SIZE");
                                col.scale = columns.getInt("DECIMAL_DIGITS");
                                col.nullable = "YES".equalsIgnoreCase(columns.getString("IS_NULLABLE"));
                                col.defaultVal = columns.getString("COLUMN_DEF");
                                table.columns.add(col);
                            }
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }

                        // PK
                        try (ResultSet primaryKeys = metaData.getPrimaryKeys(schema, schema, tableName)) {
                            Map<Short,String> order = new TreeMap<>();
                            while (primaryKeys.next()) order.put(primaryKeys.getShort("KEY_SEQ"), primaryKeys.getString("COLUMN_NAME"));
                            table.pk.addAll(order.values());
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }

                        // Indexes
                        try (ResultSet indexes = metaData.getIndexInfo(schema, schema, tableName, false, false)) {
                            Map<String, Index> idxMap = new LinkedHashMap<>();
                            while (indexes.next()) {
                                String idxName = indexes.getString("INDEX_NAME");
                                if (idxName == null) continue;
                                Index idx = idxMap.computeIfAbsent(idxName, k -> { Index x=new Index(); x.name=k;
                                    try {
                                        x.unique=!indexes.getBoolean("NON_UNIQUE");
                                    } catch (SQLException e) {
                                        throw new RuntimeException(e);
                                    }
                                    return x; });
                                idx.columns.add(indexes.getString("COLUMN_NAME"));
                            }
                            table.indexes.addAll(idxMap.values());
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }

                        catalog.tables.put(schema+"."+tableName, table);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
