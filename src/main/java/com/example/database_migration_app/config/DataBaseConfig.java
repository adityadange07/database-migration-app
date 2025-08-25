package com.example.database_migration_app.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DataBaseConfig {

    @Value("${spring.datasource.mysql.url}")
    String mysqlUrl;

    @Value("${spring.datasource.mysql.username}")
    String mysqlUser;

    @Value("${spring.datasource.mysql.password}")
    String mysqlPass;

    @Value("${spring.datasource.postgresAdmin.url}")
    String pgAdminUrl;

    @Value("${spring.datasource.postgres.username}")
    String pgUser;

    @Value("${spring.datasource.postgres.password}")
    String pgPass;

    @Value("${spring.datasource.postgres.url}")
    String pgUrl;

    @Bean("mysqlDataSource")
    public DataSource mysqlDataSource() {
        return DataSourceBuilder.create().url(mysqlUrl).username(mysqlUser).password(mysqlPass).build();
    }

    @Bean("pgAdminDataSource")
    public DataSource pgAdminDataSource() {
        return DataSourceBuilder.create().url(pgAdminUrl).username(pgUser).password(pgPass).build();
    }

    @Bean("pgDataSource")
    public DataSource pgDataSource() {
        return DataSourceBuilder.create().url(pgUrl).username(pgUser).password(pgPass).build();
    }

    @Bean("mysqlJdbc")
    public JdbcTemplate mysqlJdbc(@Qualifier("mysqlDataSource") DataSource ds) {
        var jt=new JdbcTemplate(ds);
        jt.setFetchSize(5000);
        return jt;
    }

    @Bean("pgAdminJdbc")
    public JdbcTemplate pgAdminJdbc(@Qualifier("pgAdminDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean("pgJdbc")
    public JdbcTemplate pgJdbc(@Qualifier("pgDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

}
