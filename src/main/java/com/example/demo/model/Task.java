package com.example.demo.model;

/**
 * Domain model. A record keeps it immutable; use withX helpers to produce updated copies.
 */
public record Task(Long id, String title, boolean completed) {

    public Task withId(Long newId) {
        return new Task(newId, title, completed);
    }
}
