package com.example.demo.repository;

import com.example.demo.model.Task;

import java.util.List;
import java.util.Optional;

public interface TaskRepository {

    List<Task> findAll();

    Optional<Task> findById(Long id);

    Task save(Task task);

    boolean deleteById(Long id);
}
