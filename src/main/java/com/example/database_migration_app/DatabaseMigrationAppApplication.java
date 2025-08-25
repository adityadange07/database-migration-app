package com.example.database_migration_app;

import com.example.database_migration_app.service.MysqlService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DatabaseMigrationAppApplication implements CommandLineRunner {

    private final MysqlService mysqlService;

    public DatabaseMigrationAppApplication(MysqlService mysqlService) {
        this.mysqlService = mysqlService;
    }

    public static void main(String[] args) {
		SpringApplication.run(DatabaseMigrationAppApplication.class, args);
	}

    @Override
    public void run(String... args) throws Exception {
        mysqlService.readMysqlData();
    }
}
