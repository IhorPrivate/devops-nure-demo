package com.example.demo.repository;

import com.example.demo.exception.TaskNotFoundException;
import com.example.demo.model.Task;
import com.example.demo.support.AbstractIntegrationTest;
import com.example.demo.support.SeedTasks;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcTaskRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTaskRepository repository;

    @Test
    void findAllReturnsSeedDataOrderedById() {
        assertThat(repository.findAll()).containsExactlyElementsOf(SeedTasks.all());
    }

    @Test
    void findByIdReturnsSeedTask() {
        Task seed = SeedTasks.first();

        assertThat(repository.findById(seed.id())).contains(seed);
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        assertThat(repository.findById(SeedTasks.maxId() + 100)).isEmpty();
    }

    @Test
    void saveWithoutIdInsertsWithIdAfterSeedData() {
        Task saved = repository.save(new Task(null, "New task", false));

        // Sequences are not rolled back with the test transaction, so only "after the seed ids" is stable
        assertThat(saved.id()).isGreaterThan(SeedTasks.maxId());
        assertThat(repository.findById(saved.id())).contains(saved);
    }

    @Test
    void saveWithIdUpdatesExistingRow() {
        Task seed = SeedTasks.first();
        Task changed = new Task(seed.id(), seed.title() + " (edited)", !seed.completed());

        assertThat(repository.save(changed)).isEqualTo(changed);
        assertThat(repository.findById(seed.id())).contains(changed);
    }

    @Test
    void saveWithUnknownIdThrows() {
        long missing = SeedTasks.maxId() + 100;

        assertThatThrownBy(() -> repository.save(new Task(missing, "Ghost", false)))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessage("Task with id " + missing + " not found");
    }

    @Test
    void deleteByIdRemovesRow() {
        Task seed = SeedTasks.first();

        assertThat(repository.deleteById(seed.id())).isTrue();
        assertThat(repository.findById(seed.id())).isEmpty();
        assertThat(repository.findAll()).hasSize(SeedTasks.all().size() - 1);
    }

    @Test
    void deleteByIdReturnsFalseForUnknownId() {
        assertThat(repository.deleteById(SeedTasks.maxId() + 100)).isFalse();
    }
}
