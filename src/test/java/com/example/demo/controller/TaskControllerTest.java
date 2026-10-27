package com.example.demo.controller;

import com.example.demo.model.Task;
import com.jayway.jsonpath.JsonPath;
import com.example.demo.support.AbstractIntegrationTest;
import com.example.demo.support.SeedTasks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end HTTP tests: controller, service, repository and a real PostgreSQL seeded by Liquibase.
 */
class TaskControllerTest extends AbstractIntegrationTest {

    private static final String TASKS = "/api/tasks";

    @Autowired
    private MockMvc mockMvc;

    static List<Task> seedTasks() {
        return SeedTasks.all();
    }

    private static long missingId() {
        return SeedTasks.maxId() + 100;
    }

    private static String json(String title, boolean completed) {
        return "{\"title\":\"" + title + "\",\"completed\":" + completed + "}";
    }

    private ResultActions expectTask(ResultActions actions, String path, Task task) throws Exception {
        return actions
                .andExpect(jsonPath(path + ".id", is(task.id().intValue())))
                .andExpect(jsonPath(path + ".title", is(task.title())))
                .andExpect(jsonPath(path + ".completed", is(task.completed())));
    }

    @Test
    void getAllReturnsSeedTasksInOrder() throws Exception {
        List<Task> seed = SeedTasks.all();
        ResultActions result = mockMvc.perform(get(TASKS))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(seed.size())));

        for (int i = 0; i < seed.size(); i++) {
            expectTask(result, "$[" + i + "]", seed.get(i));
        }
    }

    @ParameterizedTest
    @MethodSource("seedTasks")
    void getByIdReturnsEachSeedTask(Task task) throws Exception {
        expectTask(mockMvc.perform(get(TASKS + "/" + task.id())).andExpect(status().isOk()), "$", task);
    }

    @Test
    void getByIdReturnsProblemDetailWhenMissing() throws Exception {
        mockMvc.perform(get(TASKS + "/" + missingId()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.detail", is("Task with id " + missingId() + " not found")));
    }

    @Test
    void createReturnsCreatedTaskWithLocation() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post(TASKS).contentType(MediaType.APPLICATION_JSON).content(json("  Write report ", false)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id", greaterThan((int) SeedTasks.maxId())))
                .andExpect(jsonPath("$.title", is("Write report")))
                .andExpect(jsonPath("$.completed", is(false)))
                .andReturn().getResponse();
        String location = response.getHeader("Location");
        int id = JsonPath.read(response.getContentAsString(), "$.id");
        assertThat(location).endsWith(TASKS + "/" + id);

        mockMvc.perform(get(TASKS + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Write report")));

        mockMvc.perform(get(TASKS)).andExpect(jsonPath("$", hasSize(SeedTasks.all().size() + 1)));
    }

    @Test
    void createRejectsBlankTitle() throws Exception {
        mockMvc.perform(post(TASKS).contentType(MediaType.APPLICATION_JSON).content(json("  ", false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.detail", is("Task title must not be blank")));
    }

    @Test
    void createRejectsMissingTitle() throws Exception {
        mockMvc.perform(post(TASKS).contentType(MediaType.APPLICATION_JSON).content("{\"completed\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", is("Task title must not be blank")));
    }

    @Test
    void createRejectsMalformedJson() throws Exception {
        mockMvc.perform(post(TASKS).contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void updateChangesExistingTask() throws Exception {
        Task seed = SeedTasks.first();
        Task expected = new Task(seed.id(), "Updated title", !seed.completed());

        expectTask(mockMvc.perform(put(TASKS + "/" + seed.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(expected.title(), expected.completed())))
                .andExpect(status().isOk()), "$", expected);

        expectTask(mockMvc.perform(get(TASKS + "/" + seed.id())).andExpect(status().isOk()), "$", expected);
    }

    @Test
    void updateReturnsNotFoundForMissingTask() throws Exception {
        mockMvc.perform(put(TASKS + "/" + missingId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Title", true)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void updateRejectsBlankTitle() throws Exception {
        mockMvc.perform(put(TASKS + "/" + SeedTasks.first().id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("", true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", is("Task title must not be blank")));
    }

    @Test
    void deleteRemovesTask() throws Exception {
        long id = SeedTasks.first().id();

        mockMvc.perform(delete(TASKS + "/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get(TASKS + "/" + id)).andExpect(status().isNotFound());
        mockMvc.perform(get(TASKS)).andExpect(jsonPath("$", hasSize(SeedTasks.all().size() - 1)));
    }

    @Test
    void deleteReturnsNotFoundForMissingTask() throws Exception {
        mockMvc.perform(delete(TASKS + "/" + missingId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void unsupportedMethodReturnsProblemDetail() throws Exception {
        mockMvc.perform(put(TASKS).contentType(MediaType.APPLICATION_JSON).content(json("x", false)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status", is(405)));
    }
}
