package com.example.demo;

import com.example.demo.support.AbstractIntegrationTest;
import com.example.demo.support.SeedTasks;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DemoApplicationTests extends AbstractIntegrationTest {

    @Autowired
    private JdbcClient jdbc;

    @Test
    void contextLoads() {
    }

    @Test
    void liquibaseAppliedAllChangeSets() {
        List<String> applied = jdbc.sql("SELECT id FROM databasechangelog ORDER BY orderexecuted")
                .query(String.class)
                .list();

        assertThat(applied).containsExactly(
                "001-create-tasks-table",
                "002-load-seed-tasks",
                "003-sync-tasks-id-sequence");
    }

    @Test
    void liquibaseLoadedEverySeedRow() {
        Integer count = jdbc.sql("SELECT COUNT(*) FROM tasks").query(Integer.class).single();

        assertThat(count).isEqualTo(SeedTasks.all().size());
    }
}
