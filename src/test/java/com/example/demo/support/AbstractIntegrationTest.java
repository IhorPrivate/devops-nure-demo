package com.example.demo.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * Common setup for tests against a real PostgreSQL migrated by Liquibase.
 * All subclasses share one Spring context and one container. Each test runs in a transaction that is
 * rolled back afterwards, so the seed data is identical at the start of every test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
public abstract class AbstractIntegrationTest {
}
