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
            try (ResultSet rs = metaData.getCatalogs()) {
                while (rs.next()) {
                    String s = rs.getString("TABLE_CAT");
                    if (s == null) {
                        continue;
                    }
                    schemas.add(s);
                }
            }
            for (String schema : schemas) {
                try (ResultSet rs = metaData.getTables(null, schema, "%", new String[]{"TABLE"})) {
                    while (rs.next()) {
                        String tableName = rs.getString("TABLE_NAME");

                        Table table = new Table();
                        table.schema = schema;
                        table.name = tableName;

                        // Columns
                        try (ResultSet crs = metaData.getColumns(null, schema, tableName, "%")) {
                            while (crs.next()) {
                                Column col = new Column();
                                col.name = crs.getString("COLUMN_NAME");
                                col.mysqlType = crs.getString("TYPE_NAME");
                                col.size = crs.getInt("COLUMN_SIZE");
                                col.scale = crs.getInt("DECIMAL_DIGITS");
                                col.nullable = "YES".equalsIgnoreCase(crs.getString("IS_NULLABLE"));
                                col.defaultVal = crs.getString("COLUMN_DEF");
                                table.columns.add(col);
                            }
                        }

                        // PK
                        try (ResultSet prs = metaData.getPrimaryKeys(null, schema, tableName)) {
                            Map<Short,String> order = new TreeMap<>();
                            while (prs.next()) order.put(prs.getShort("KEY_SEQ"), prs.getString("COLUMN_NAME"));
                            table.pk.addAll(order.values());
                        }

                        // Indexes
                        try (ResultSet irs = metaData.getIndexInfo(null, schema, tableName, false, false)) {
                            Map<String, Index> idxMap = new LinkedHashMap<>();
                            while (irs.next()) {
                                String idxName = irs.getString("INDEX_NAME");
                                if (idxName == null) continue;
                                Index idx = idxMap.computeIfAbsent(idxName, k -> { Index x=new Index(); x.name=k;
                                    try {
                                        x.unique=!irs.getBoolean("NON_UNIQUE");
                                    } catch (SQLException e) {
                                        throw new RuntimeException(e);
                                    }
                                    return x; });
                                idx.columns.add(irs.getString("COLUMN_NAME"));
                            }
                            table.indexes.addAll(idxMap.values());
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
