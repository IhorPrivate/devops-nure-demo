package com.example.demo.support;

import com.example.demo.model.Task;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Reads the same CSV that Liquibase loads into the database, so tests assert against the real seed data
 * instead of a hand-copied duplicate.
 */
public final class SeedTasks {

    public static final String RESOURCE = "db/changelog/data/tasks.csv";

    private static final List<Task> TASKS = read();

    private SeedTasks() {
    }

    public static List<Task> all() {
        return TASKS;
    }

    public static Task first() {
        return TASKS.getFirst();
    }

    public static long maxId() {
        return TASKS.stream().mapToLong(Task::id).max().orElse(0);
    }

    private static List<Task> read() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(RESOURCE).getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .skip(1) // header: id;title;completed
                    .filter(line -> !line.isBlank())
                    .map(SeedTasks::parse)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + RESOURCE, e);
        }
    }

    private static Task parse(String line) {
        String[] cols = line.split(";", -1);
        return new Task(Long.parseLong(cols[0].trim()), cols[1].trim(), Boolean.parseBoolean(cols[2].trim()));
    }
}
