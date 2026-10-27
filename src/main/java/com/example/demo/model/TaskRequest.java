package com.example.demo.model;

/**
 * Incoming payload for create/update. Kept separate from the entity so clients cannot set the id.
 */
public record TaskRequest(String title, boolean completed) {
}
