package com.example.demo.service;

import com.example.demo.exception.TaskNotFoundException;
import com.example.demo.model.Task;
import com.example.demo.model.TaskRequest;
import com.example.demo.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Business logic layer: validation and orchestration between controller and repository.
 */
@Service
public class TaskService {

    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    public List<Task> findAll() {
        return repository.findAll();
    }

    public Task findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    public Task create(TaskRequest request) {
        validate(request);
        return repository.save(new Task(null, request.title().trim(), request.completed()));
    }

    public Task update(Long id, TaskRequest request) {
        validate(request);
        findById(id); // throws 404 if missing
        return repository.save(new Task(id, request.title().trim(), request.completed()));
    }

    public void delete(Long id) {
        if (!repository.deleteById(id)) {
            throw new TaskNotFoundException(id);
        }
    }

    private void validate(TaskRequest request) {
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("Task title must not be blank");
        }
    }
}
