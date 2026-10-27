package com.example.demo.service;

import com.example.demo.exception.TaskNotFoundException;
import com.example.demo.model.Task;
import com.example.demo.model.TaskRequest;
import com.example.demo.repository.TaskRepository;
import com.example.demo.support.SeedTasks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests: the repository is mocked, so only the business rules in TaskService are exercised.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final String BLANK_TITLE = "Task title must not be blank";

    @Mock
    private TaskRepository repository;

    @InjectMocks
    private TaskService service;

    @Test
    void findAllDelegatesToRepository() {
        when(repository.findAll()).thenReturn(SeedTasks.all());

        assertThat(service.findAll()).isEqualTo(SeedTasks.all());
    }

    @Test
    void findByIdReturnsTask() {
        Task seed = SeedTasks.first();
        when(repository.findById(seed.id())).thenReturn(Optional.of(seed));

        assertThat(service.findById(seed.id())).isEqualTo(seed);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(42L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessage("Task with id 42 not found");
    }

    @Test
    void createTrimsTitleAndSavesWithoutId() {
        Task saved = new Task(11L, "Write report", true);
        when(repository.save(new Task(null, "Write report", true))).thenReturn(saved);

        assertThat(service.create(new TaskRequest("  Write report  ", true))).isEqualTo(saved);
    }

    @Test
    void createRejectsNullRequest() {
        assertThatThrownBy(() -> service.create(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(BLANK_TITLE);
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t"})
    void createRejectsMissingOrBlankTitle(String title) {
        assertThatThrownBy(() -> service.create(new TaskRequest(title, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(BLANK_TITLE);
        verify(repository, never()).save(any());
    }

    @Test
    void updateSavesTrimmedTaskWithSameId() {
        Task seed = SeedTasks.first();
        Task expected = new Task(seed.id(), "Renamed", !seed.completed());
        when(repository.findById(seed.id())).thenReturn(Optional.of(seed));
        when(repository.save(expected)).thenReturn(expected);

        assertThat(service.update(seed.id(), new TaskRequest(" Renamed ", !seed.completed()))).isEqualTo(expected);
    }

    @Test
    void updateThrowsWhenMissingAndDoesNotSave() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(42L, new TaskRequest("Title", false)))
                .isInstanceOf(TaskNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void updateValidatesBeforeLookingUp() {
        assertThatThrownBy(() -> service.update(1L, new TaskRequest(" ", false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(BLANK_TITLE);
        verify(repository, never()).findById(any());
    }

    @Test
    void deleteRemovesExistingTask() {
        when(repository.deleteById(1L)).thenReturn(true);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(repository.deleteById(42L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(42L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessage("Task with id 42 not found");
    }
}
