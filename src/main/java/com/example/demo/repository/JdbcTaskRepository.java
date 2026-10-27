package com.example.demo.repository;

import com.example.demo.exception.TaskNotFoundException;
import com.example.demo.model.Task;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PostgreSQL-backed storage. Schema and seed data are managed by Liquibase
 * ({@code db/changelog/db.changelog-master.yaml}).
 */
@Repository
public class JdbcTaskRepository implements TaskRepository {

    private final JdbcClient jdbc;

    public JdbcTaskRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Task> findAll() {
        return jdbc.sql("SELECT id, title, completed FROM tasks ORDER BY id")
                .query(Task.class)
                .list();
    }

    @Override
    public Optional<Task> findById(Long id) {
        return jdbc.sql("SELECT id, title, completed FROM tasks WHERE id = :id")
                .param("id", id)
                .query(Task.class)
                .optional();
    }

    /**
     * Inserts when the task has no id (the database assigns one), otherwise updates the existing row.
     */
    @Override
    public Task save(Task task) {
        if (task.id() == null) {
            Long id = jdbc.sql("INSERT INTO tasks (title, completed) VALUES (:title, :completed) RETURNING id")
                    .param("title", task.title())
                    .param("completed", task.completed())
                    .query(Long.class)
                    .single();
            return task.withId(id);
        }
        int updated = jdbc.sql("UPDATE tasks SET title = :title, completed = :completed WHERE id = :id")
                .param("title", task.title())
                .param("completed", task.completed())
                .param("id", task.id())
                .update();
        if (updated == 0) {
            throw new TaskNotFoundException(task.id());
        }
        return task;
    }

    @Override
    public boolean deleteById(Long id) {
        return jdbc.sql("DELETE FROM tasks WHERE id = :id")
                .param("id", id)
                .update() > 0;
    }
}
